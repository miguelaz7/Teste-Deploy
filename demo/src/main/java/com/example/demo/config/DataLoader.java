package com.example.demo.config;

import com.example.demo.service.CsvImportService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements ApplicationRunner {

    private final CsvImportService csvImportService;

    public DataLoader(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        csvImportService.importCsv();
    }
}