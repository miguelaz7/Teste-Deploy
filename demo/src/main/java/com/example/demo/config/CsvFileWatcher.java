package com.example.demo.config;

import com.example.demo.service.CsvImportService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

@Component
public class CsvFileWatcher {

    private static final Logger logger = LoggerFactory.getLogger(CsvFileWatcher.class);
    private static final long DEBOUNCE_MS = 400;

    private final CsvImportService csvImportService;
    private final boolean watcherEnabled;
    private final Path csvPath;

    private volatile boolean running;
    private volatile long lastSyncAt;
    private WatchService watchService;
    private Thread watcherThread;

    public CsvFileWatcher(
        CsvImportService csvImportService,
        @Value("${app.csv.watcher-enabled:true}") boolean watcherEnabled,
        @Value("${app.csv.file-path:src/main/resources/validacoesPrototipo.csv}") String csvFilePath
    ) {
        this.csvImportService = csvImportService;
        this.watcherEnabled = watcherEnabled;
        this.csvPath = Paths.get(csvFilePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void startWatcher() {
        if (!watcherEnabled) {
            logger.info("CSV watcher desativado por configuracao.");
            return;
        }

        Path parent = csvPath.getParent();
        if (parent == null || !Files.exists(parent)) {
            logger.warn("CSV watcher nao iniciado: pasta nao encontrada: {}", parent);
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            parent.register(
                watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY
            );

            running = true;
            watcherThread = new Thread(this::watchLoop, "csv-file-watcher");
            watcherThread.setDaemon(true);
            watcherThread.start();

            logger.info("CSV watcher ativo em {}", csvPath);
        } catch (IOException e) {
            logger.error("Falha ao iniciar CSV watcher.", e);
        }
    }

    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.take();
                boolean csvChanged = false;

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changedPath = (Path) event.context();
                    if (changedPath != null && changedPath.getFileName().equals(csvPath.getFileName())) {
                        csvChanged = true;
                    }
                }

                key.reset();

                if (csvChanged) {
                    triggerSync();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException e) {
                return;
            } catch (Exception e) {
                logger.error("Erro no loop do CSV watcher.", e);
            }
        }
    }

    private void triggerSync() {
        long now = System.currentTimeMillis();
        if (now - lastSyncAt < DEBOUNCE_MS) {
            return;
        }

        lastSyncAt = now;
        try {
            csvImportService.syncCsvToDatabase();
            logger.info("CSV alterado. Base de dados sincronizada.");
        } catch (Exception e) {
            logger.error("Falha ao sincronizar base de dados apos alteracao do CSV.", e);
        }
    }

    @PreDestroy
    public void stopWatcher() {
        running = false;

        if (watcherThread != null) {
            watcherThread.interrupt();
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("Erro ao fechar CSV watcher.", e);
            }
        }
    }
}
