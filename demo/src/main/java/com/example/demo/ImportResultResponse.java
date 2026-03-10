package com.example.demo;

import java.util.ArrayList;
import java.util.List;

public class ImportResultResponse {

    private final int totalRows;
    private final int importedRows;
    private final int duplicateRows;
    private final int invalidRows;
    private final List<String> errors;

    public ImportResultResponse(int totalRows, int importedRows, int duplicateRows, int invalidRows, List<String> errors) {
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.duplicateRows = duplicateRows;
        this.invalidRows = invalidRows;
        this.errors = errors == null ? new ArrayList<>() : errors;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImportedRows() {
        return importedRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public List<String> getErrors() {
        return errors;
    }
}
