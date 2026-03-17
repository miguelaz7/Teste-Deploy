package com.example.demo.service;

import com.example.demo.model.Validation;
import com.example.demo.repository.ValidationRepository;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    private final ValidationRepository validationRepository;

    public CsvImportService(ValidationRepository validationRepository) {
        this.validationRepository = validationRepository;
    }

    public void importCsv() throws Exception {
        if (validationRepository.count() > 0) return; // evita duplicados

        InputStream is = getClass().getClassLoader().getResourceAsStream("validacoesPrototipo.csv");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));

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
        validationRepository.saveAll(list);
    }
}