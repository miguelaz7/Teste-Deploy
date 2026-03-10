package com.example.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TicketingImportService {

    private final ViagemRepository viagemRepository;
    private final ValidacaoBilheteRepository validacaoBilheteRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public TicketingImportService(ViagemRepository viagemRepository, ValidacaoBilheteRepository validacaoBilheteRepository, ObjectMapper objectMapper) {
        this.viagemRepository = viagemRepository;
        this.validacaoBilheteRepository = validacaoBilheteRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportResultResponse importFromPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return new ImportResultResponse(0, 0, 0, 0, List.of("O caminho do ficheiro e obrigatorio."));
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return new ImportResultResponse(0, 0, 0, 0, List.of("Ficheiro nao encontrado: " + filePath));
        }

        try {
            String extension = getExtension(path.getFileName().toString());
            String content = Files.readString(path, StandardCharsets.UTF_8);
            // Accept both CSV and JSON with the same business fields.
            List<Map<String, String>> rows = "json".equals(extension) ? parseJson(content) : parseCsv(content);
            return persistRows(rows);
        } catch (Exception ex) {
            return new ImportResultResponse(0, 0, 0, 0, List.of("Erro ao processar ficheiro: " + ex.getMessage()));
        }
    }

    private ImportResultResponse persistRows(List<Map<String, String>> rows) {
        int imported = 0;
        int duplicates = 0;
        int invalid = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);

            try {
                String viagemId = required(row, "viagem_id");
                String rota = required(row, "rota");
                LocalDateTime dataHora = parseDateTime(required(row, "data_hora"));
                Integer numeroPassageiros = parseInteger(required(row, "numero_passageiros"));
                String tipoBilhete = required(row, "tipo_bilhete");
                BigDecimal valorPago = parseBigDecimal(required(row, "valor_pago"));
                String autocarroId = required(row, "autocarro_id");
                String tipoBilheteNormalizado = tipoBilhete.toLowerCase(Locale.ROOT);

                Viagem viagem = viagemRepository.findByViagemId(viagemId).orElseGet(Viagem::new);
                viagem.setViagemId(viagemId);
                viagem.setRota(rota);
                viagem.setDataHora(dataHora);
                viagem.setAutocarroId(autocarroId);
                Viagem savedViagem = viagemRepository.save(viagem);

                boolean alreadyExists = validacaoBilheteRepository.existsByViagemAndNumeroPassageirosAndTipoBilheteAndValorPago(
                        savedViagem,
                        numeroPassageiros,
                        tipoBilheteNormalizado,
                        valorPago
                );

                if (alreadyExists) {
                    duplicates++;
                    continue;
                }

                // Each valid row becomes one ticket validation linked to its trip.
                ValidacaoBilhete validacao = new ValidacaoBilhete();
                validacao.setViagem(savedViagem);
                validacao.setNumeroPassageiros(numeroPassageiros);
                validacao.setTipoBilhete(tipoBilheteNormalizado);
                validacao.setValorPago(valorPago);
                validacaoBilheteRepository.save(validacao);

                imported++;
            } catch (Exception ex) {
                invalid++;
                if (errors.size() < 25) {
                    errors.add("Linha " + (i + 1) + ": " + ex.getMessage());
                }
            }
        }

        return new ImportResultResponse(rows.size(), imported, duplicates, invalid, errors);
    }

    private List<Map<String, String>> parseJson(String content) throws IOException {
        List<Map<String, Object>> source = objectMapper.readValue(content, new TypeReference<>() {});
        List<Map<String, String>> result = new ArrayList<>();

        for (Map<String, Object> row : source) {
            Map<String, String> normalized = new HashMap<>();
            row.forEach((key, value) -> normalized.put(key, value == null ? "" : String.valueOf(value)));
            result.add(normalized);
        }

        return result;
    }

    private List<Map<String, String>> parseCsv(String content) {
        List<Map<String, String>> rows = new ArrayList<>();
        String[] lines = content.split("\\r?\\n");
        if (lines.length == 0) {
            return rows;
        }

        String[] headers = splitCsvLine(lines[0]);
        for (int lineIndex = 1; lineIndex < lines.length; lineIndex++) {
            if (lines[lineIndex].isBlank()) {
                continue;
            }

            String[] columns = splitCsvLine(lines[lineIndex]);
            Map<String, String> row = new HashMap<>();
            for (int colIndex = 0; colIndex < headers.length; colIndex++) {
                String key = headers[colIndex].trim();
                String value = colIndex < columns.length ? columns[colIndex].trim() : "";
                row.put(key, stripQuotes(value));
            }
            rows.add(row);
        }

        return rows;
    }

    private String[] splitCsvLine(String line) {
        return line.split(";|,");
    }

    private String stripQuotes(String value) {
        return value.replace("\"", "").trim();
    }

    private String required(Map<String, String> row, String field) {
        String value = row.get(field);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo em falta: " + field);
        }
        return value.trim();
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data/hora invalida (usar formato YYYY-MM-DDTHH:MM:SS): " + value);
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Numero de passageiros invalido: " + value);
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valor pago invalido: " + value);
        }
    }

    private String getExtension(String fileName) {
        if (!fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
