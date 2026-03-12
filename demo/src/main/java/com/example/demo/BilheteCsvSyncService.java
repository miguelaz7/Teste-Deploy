package com.example.demo;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BilheteCsvSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BilheteCsvSyncService.class);

    private final BilheteRepository bilheteRepository;
    private final BilheteRealtimeService realtimeService;
    private final ExecutorService watcherExecutor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService pollExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Path csvPath;
    private volatile boolean running = true;
    private WatchService watchService;
    private volatile FileTime lastKnownModifiedTime;

    public BilheteCsvSyncService(
        BilheteRepository bilheteRepository,
        BilheteRealtimeService realtimeService,
        @Value("${bilhetes.csv.path:./data/bilhetes.csv}") String csvPath
    ) {
        this.bilheteRepository = bilheteRepository;
        this.realtimeService = realtimeService;
        this.csvPath = Path.of(csvPath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void start() {
        if (Files.exists(csvPath)) {
            reloadFromCsv();
            updateLastModifiedSnapshot();
        } else {
            LOGGER.warn("CSV de bilhetes nao encontrado em {}. A monitorizacao sera iniciada assim que o ficheiro existir.", csvPath);
        }

        try {
            startWatcher();
            startPollingFallback();
        } catch (IOException exception) {
            LOGGER.error("Falha ao iniciar monitorizacao do CSV {}", csvPath, exception);
        }
    }

    @PreDestroy
    public void stop() {
        running = false;

        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException exception) {
            LOGGER.warn("Erro ao fechar watcher de CSV.", exception);
        }

        watcherExecutor.shutdownNow();
        pollExecutor.shutdownNow();
    }

    private void startWatcher() throws IOException {
        Path directory = Objects.requireNonNull(csvPath.getParent(), "Diretorio do CSV nao pode ser nulo.");
        Files.createDirectories(directory);

        watchService = FileSystems.getDefault().newWatchService();
        directory.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY
        );

        watcherExecutor.submit(() -> {
            while (running) {
                WatchKey key;
                try {
                    key = watchService.take();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception exception) {
                    if (running) {
                        LOGGER.error("Watcher de CSV interrompido com erro.", exception);
                    }
                    break;
                }

                boolean csvChanged = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changedFile = (Path) event.context();
                    if (changedFile != null && changedFile.getFileName().equals(csvPath.getFileName())) {
                        csvChanged = true;
                        break;
                    }
                }

                key.reset();

                if (csvChanged) {
                    // Pequeno atraso para evitar leitura de ficheiro ainda em escrita.
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    reloadFromCsv();
                    updateLastModifiedSnapshot();
                }
            }
        });

        LOGGER.info("Monitorizacao de bilhetes ativa para {}", csvPath);
    }

    private void startPollingFallback() {
        pollExecutor.scheduleWithFixedDelay(() -> {
            if (!running) {
                return;
            }

            try {
                if (!Files.exists(csvPath)) {
                    return;
                }

                FileTime currentModifiedTime = Files.getLastModifiedTime(csvPath);
                FileTime knownModifiedTime = lastKnownModifiedTime;

                if (knownModifiedTime == null || currentModifiedTime.compareTo(knownModifiedTime) > 0) {
                    reloadFromCsv();
                    lastKnownModifiedTime = currentModifiedTime;
                }
            } catch (Exception exception) {
                LOGGER.warn("Falha no polling de fallback do CSV de bilhetes.", exception);
            }
        }, 2, 2, TimeUnit.SECONDS);
    }

    @Transactional
    public synchronized void reloadFromCsv() {
        try {
            if (!Files.exists(csvPath)) {
                LOGGER.warn("Ignorado reload de bilhetes: ficheiro {} nao existe.", csvPath);
                return;
            }

            List<String> lines = Files.readAllLines(csvPath)
                .stream()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .collect(Collectors.toList());

            if (lines.size() < 2) {
                LOGGER.warn("CSV de bilhetes sem dados validos em {}", csvPath);
                bilheteRepository.deleteAllInBatch();
                realtimeService.broadcastBilhetesAtualizados(0);
                return;
            }

            char separator = detectSeparator(lines.get(0));
            Map<String, Integer> headerMap = parseHeader(lines.get(0), separator);

            List<Bilhete> bilhetes = new ArrayList<>();
            Instant now = Instant.now();

            for (int index = 1; index < lines.size(); index++) {
                String line = lines.get(index);
                String[] columns = splitCsvLine(line, separator);

                String codigo = getColumn(columns, headerMap, "codigo");
                String nome = getColumn(columns, headerMap, "nome");
                String precoRaw = getColumn(columns, headerMap, "preco");
                String quantidadeRaw = getColumn(columns, headerMap, "quantidade");

                if (codigo == null || nome == null || precoRaw == null || quantidadeRaw == null) {
                    LOGGER.warn("Linha {} ignorada por dados em falta: {}", index + 1, line);
                    continue;
                }

                BigDecimal preco;
                Integer quantidade;
                try {
                    preco = new BigDecimal(precoRaw.replace(',', '.'));
                    quantidade = Integer.valueOf(quantidadeRaw);
                } catch (NumberFormatException exception) {
                    LOGGER.warn("Linha {} ignorada por formato invalido: {}", index + 1, line);
                    continue;
                }

                bilhetes.add(new Bilhete(codigo, nome, preco, quantidade, now));
            }

            bilheteRepository.deleteAllInBatch();
            bilheteRepository.saveAll(bilhetes);
            realtimeService.broadcastBilhetesAtualizados(bilhetes.size());

            LOGGER.info("Bilhetes recarregados do CSV. Total: {}", bilhetes.size());
        } catch (IOException exception) {
            LOGGER.error("Falha ao ler CSV de bilhetes em {}", csvPath, exception);
        }
    }

    private char detectSeparator(String headerLine) {
        return headerLine.contains(";") ? ';' : ',';
    }

    private Map<String, Integer> parseHeader(String headerLine, char separator) {
        String[] headers = splitCsvLine(headerLine, separator);

        return Map.of(
            "codigo", findHeaderIndex(headers, "codigo", "cod", "id"),
            "nome", findHeaderIndex(headers, "nome", "titulo", "descricao"),
            "preco", findHeaderIndex(headers, "preco", "price"),
            "quantidade", findHeaderIndex(headers, "quantidade", "stock", "disponiveis")
        );
    }

    private int findHeaderIndex(String[] headers, String... candidates) {
        for (int index = 0; index < headers.length; index++) {
            String current = normalizeHeader(headers[index]);
            for (String candidate : candidates) {
                if (current.equals(candidate)) {
                    return index;
                }
            }
        }
        return -1;
    }

    private String normalizeHeader(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String getColumn(String[] columns, Map<String, Integer> headerMap, String header) {
        Integer index = headerMap.get(header);
        if (index == null || index < 0 || index >= columns.length) {
            return null;
        }

        String value = columns[index].trim();
        return value.isEmpty() ? null : value;
    }

    private String[] splitCsvLine(String line, char separator) {
        return line.split(Pattern.quote(String.valueOf(separator)), -1);
    }

    private void updateLastModifiedSnapshot() {
        try {
            if (Files.exists(csvPath)) {
                lastKnownModifiedTime = Files.getLastModifiedTime(csvPath);
            }
        } catch (IOException exception) {
            LOGGER.warn("Falha ao atualizar timestamp conhecido do CSV.", exception);
        }
    }
}
