package com.example.demo.service;

import com.example.demo.dto.CsvImportResultDto;
import com.example.demo.model.CsvIngestionCursor;
import com.example.demo.repository.CsvIngestionCursorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CsvRealtimeIngestionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(CsvRealtimeIngestionScheduler.class);

    private final CsvNormalizationService csvNormalizationService;
    private final CsvIngestionCursorRepository csvIngestionCursorRepository;

    @Value("${app.csv.realtime.enabled:true}")
    private boolean realtimeEnabled;

    @Value("${app.csv.realtime.path:}")
    private String realtimeCsvPath;

    public CsvRealtimeIngestionScheduler(
        CsvNormalizationService csvNormalizationService,
        CsvIngestionCursorRepository csvIngestionCursorRepository
    ) {
        this.csvNormalizationService = csvNormalizationService;
        this.csvIngestionCursorRepository = csvIngestionCursorRepository;
    }

    @Scheduled(
        fixedDelayString = "${app.csv.realtime.poll-ms:3000}",
        initialDelayString = "${app.csv.realtime.initial-delay-ms:5000}"
    )
    public synchronized void processarNovasLinhasCsv() {
        if (!realtimeEnabled) {
            return;
        }
        if (realtimeCsvPath == null || realtimeCsvPath.isBlank()) {
            return;
        }

        Path csvPath = Paths.get(realtimeCsvPath);
        if (!Files.exists(csvPath)) {
            LOG.debug("CSV realtime ainda nao encontrado em {}", realtimeCsvPath);
            return;
        }

        try {
            Snapshot snapshot = lerNovasLinhas(csvPath);
            if (snapshot == null || snapshot.newLines().isEmpty()) {
                return;
            }

            StringBuilder payload = new StringBuilder(snapshot.header());
            for (String line : snapshot.newLines()) {
                payload.append('\n').append(line);
            }

            try (BufferedReader reader = new BufferedReader(new StringReader(payload.toString()))) {
                CsvImportResultDto resultado = csvNormalizationService.importarCsvNormalizado(reader);
                salvarCursor(csvPath, snapshot.totalDataLines(), snapshot.fileSize(), snapshot.lastModifiedAt());
                LOG.info(
                    "Realtime CSV: processadas {} novas linhas (validas={}, quarentena={}, duplicados={})",
                    snapshot.newLines().size(),
                    resultado.getValidosPersistidos(),
                    resultado.getEmQuarentena(),
                    resultado.getDuplicadosDescartados()
                );
            }
        } catch (Exception e) {
            LOG.error("Falha no processamento realtime do CSV em {}", realtimeCsvPath, e);
        }
    }

    private Snapshot lerNovasLinhas(Path csvPath) throws IOException {
        List<String> newLines = new ArrayList<>();
        String header;
        long totalDataLines = 0;
        long fileSize = Files.size(csvPath);
        long lastModifiedAt = Files.getLastModifiedTime(csvPath).toMillis();

        CsvIngestionCursor cursor = carregarCursor(csvPath).orElseGet(() -> new CsvIngestionCursor(csvPath.toString(), 0, 0, 0, java.time.OffsetDateTime.now()));

        if (fileSize < cursor.getLastKnownSize()) {
            LOG.info("CSV realtime foi truncado/rodado. Reiniciar leitura incremental.");
            cursor.setProcessedDataLines(0);
        } else if (fileSize == cursor.getLastKnownSize() && lastModifiedAt == cursor.getLastKnownModifiedAt()) {
            return new Snapshot(null, List.of(), 0, fileSize, lastModifiedAt);
        }

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            header = reader.readLine();
            if (header == null) {
                return null;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                totalDataLines++;
                if (totalDataLines > cursor.getProcessedDataLines()) {
                    newLines.add(line);
                }
            }
        }

        return new Snapshot(header, newLines, totalDataLines, fileSize, lastModifiedAt);
    }

    private Optional<CsvIngestionCursor> carregarCursor(Path csvPath) {
        return csvIngestionCursorRepository.findByCsvPath(csvPath.toString());
    }

    private void salvarCursor(Path csvPath, long processedDataLines, long lastKnownSize, long lastKnownModifiedAt) {
        CsvIngestionCursor cursor = csvIngestionCursorRepository.findByCsvPath(csvPath.toString())
            .orElseGet(() -> new CsvIngestionCursor(csvPath.toString(), 0, 0, 0, java.time.OffsetDateTime.now()));
        cursor.setCsvPath(csvPath.toString());
        cursor.setProcessedDataLines(processedDataLines);
        cursor.setLastKnownSize(lastKnownSize);
        cursor.setLastKnownModifiedAt(lastKnownModifiedAt);
        cursor.setUpdatedAt(java.time.OffsetDateTime.now());
        csvIngestionCursorRepository.save(cursor);
    }

    private record Snapshot(String header, List<String> newLines, long totalDataLines, long fileSize, long lastModifiedAt) {
    }
}
