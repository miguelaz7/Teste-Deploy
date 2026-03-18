package com.example.demo.service;

import com.example.demo.model.Validation;
import com.example.demo.repository.ValidationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    private final ValidationRepository validationRepository;
    private final Path csvPath;

    public CsvImportService(
        ValidationRepository validationRepository,
        @Value("${app.csv.file-path:src/main/resources/validacoesPrototipo.csv}") String csvFilePath
    ) {
        this.validationRepository = validationRepository;
        this.csvPath = Paths.get(csvFilePath).toAbsolutePath().normalize();
    }

    public void importCsv() throws Exception {
        if (validationRepository.count() > 0) {
            return;
        }

        try (BufferedReader reader = getClasspathCsvReader()) {
            validationRepository.saveAll(parseCsv(reader));
        }
    }

    @Transactional
    public void syncCsvToDatabase() throws Exception {
        try (BufferedReader reader = getCsvReaderForSync()) {
            List<Validation> list = parseCsv(reader);
            validationRepository.deleteAllInBatch();
            validationRepository.saveAll(list);
        }
    }

    private BufferedReader getClasspathCsvReader() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("validacoesPrototipo.csv");
        if (is == null) {
            throw new IllegalStateException("Recurso validacoesPrototipo.csv nao encontrado no classpath.");
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private BufferedReader getCsvReaderForSync() throws IOException {
        if (Files.exists(csvPath)) {
            return Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
        }
        return getClasspathCsvReader();
    }

    private List<Validation> parseCsv(BufferedReader reader) throws IOException {
        List<Validation> list = new ArrayList<>();
        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            if (firstLine) { firstLine = false; continue; } // salta cabeçalho
            String[] parts = line.split(",");
            Validation v = new Validation(
                LocalDateTime.parse(parts[0]),
                parts[1],
                parts[2],
                Double.parseDouble(parts[3]),
                Double.parseDouble(parts[4]),
                parts[5],
                Boolean.parseBoolean(parts[6])
            );
            list.add(v);
        }
        return list;
    }
}