package com.example.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.dto.CsvImportResultDto;
import com.example.demo.dto.NgsiLdEntityDto;
import com.example.demo.model.FareCollectionSystem;
import com.example.demo.model.IngestionAuditLog;
import com.example.demo.model.IngestionBatchControl;
import com.example.demo.model.IngestionRetryQueue;
import com.example.demo.model.NgsiLdDataLakeRecord;
import com.example.demo.model.Stop;
import com.example.demo.model.TicketType;
import com.example.demo.model.ValidationEvent;
import com.example.demo.model.ValidationQuarantine;
import com.example.demo.repository.FareCollectionSystemRepository;
import com.example.demo.repository.IngestionAuditLogRepository;
import com.example.demo.repository.IngestionBatchControlRepository;
import com.example.demo.repository.IngestionRetryQueueRepository;
import com.example.demo.repository.NgsiLdDataLakeRecordRepository;
import com.example.demo.repository.RouteRepository;
import com.example.demo.repository.StopRepository;
import com.example.demo.repository.StopTimesRepository;
import com.example.demo.repository.TicketTypeRepository;
import com.example.demo.repository.TripRepository;
import com.example.demo.repository.ValidationEventRepository;
import com.example.demo.repository.ValidationQuarantineRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CsvNormalizationService {

    private static final Logger LOG = LoggerFactory.getLogger(CsvNormalizationService.class);
    private static final Duration JANELA_DUPLICADOS = Duration.ofHours(24);
    private static final Duration LIMITE_PASSADO = Duration.ofDays(90);
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);
    private static final long LIMITE_VALIDACAO_MS = 5_000;
    private static final long LIMITE_NORMALIZACAO_MS = 10_000;
    private static final long LIMITE_PERSISTENCIA_MS = 15_000;

    private static final Set<String> SCB_HEADERS_ESPERADOS = Set.of(
        "cardId", "ticketId", "mediaType", "ticketTypeCode", "transactionType", "transactionDateTime",
        "originStopId", "route_id", "trip_id", "fareForAdult", "equipmentId", "transactionVehicleNum",
        "result", "reject_reason"
    );

    private static final Map<String, String> TICKET_TYPES_CANONICOS = Map.ofEntries(
        Map.entry("PASSE_ESTUDANTE", "PASSE_ESTUDANTE"),
        Map.entry("ESTUDANTE", "PASSE_ESTUDANTE"),
        Map.entry("SINGLE_TICKET", "AVULSO"),
        Map.entry("AVULSO", "AVULSO"),
        Map.entry("MONTHLY_PASS", "MENSAL"),
        Map.entry("MENSAL", "MENSAL"),
        Map.entry("SENIOR_PASS", "PASSE_SENIOR"),
        Map.entry("PASSE_SENIOR", "PASSE_SENIOR")
    );

    private final StopRepository stopRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final StopTimesRepository stopTimesRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final ValidationEventRepository validationEventRepository;
    private final FareCollectionSystemRepository fareCollectionSystemRepository;
    private final ValidationQuarantineRepository validationQuarantineRepository;
    private final IngestionAuditLogRepository ingestionAuditLogRepository;
    private final NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository;
    private final IngestionBatchControlRepository ingestionBatchControlRepository;
    private final IngestionRetryQueueRepository ingestionRetryQueueRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.csv.file-name:validacoesBus.csv}")
    private String csvFileName;

    @Value("${app.ingestion.pseudonym.secret:dev-secret-change-me}")
    private String pseudonymSecret;

    @Value("${app.etl.mapping-version:v1.0.0}")
    private String etlMappingVersion;

    @Value("${app.ingestion.retry.interval-ms:30000}")
    private long retryIntervalMs;

    public CsvNormalizationService(
        StopRepository stopRepository,
        RouteRepository routeRepository,
        TripRepository tripRepository,
        StopTimesRepository stopTimesRepository,
        TicketTypeRepository ticketTypeRepository,
        ValidationEventRepository validationEventRepository,
        FareCollectionSystemRepository fareCollectionSystemRepository,
        ValidationQuarantineRepository validationQuarantineRepository,
        IngestionAuditLogRepository ingestionAuditLogRepository,
        NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository,
        IngestionBatchControlRepository ingestionBatchControlRepository,
        IngestionRetryQueueRepository ingestionRetryQueueRepository
    ) {
        this.stopRepository = stopRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.stopTimesRepository = stopTimesRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.validationEventRepository = validationEventRepository;
        this.fareCollectionSystemRepository = fareCollectionSystemRepository;
        this.validationQuarantineRepository = validationQuarantineRepository;
        this.ingestionAuditLogRepository = ingestionAuditLogRepository;
        this.ngsiLdDataLakeRecordRepository = ngsiLdDataLakeRecordRepository;
        this.ingestionBatchControlRepository = ingestionBatchControlRepository;
        this.ingestionRetryQueueRepository = ingestionRetryQueueRepository;
    }

    @Transactional
    public CsvImportResultDto importarCsvNormalizado() throws IOException {
        try (BufferedReader reader = getClasspathCsvReader()) {
            return importarCsvNormalizado(reader);
        }
    }

    @Transactional
    public CsvImportResultDto importarCsvNormalizado(BufferedReader reader) throws IOException {
        long inicioTotal = System.currentTimeMillis();
        long inicioValidacao = inicioTotal;

        List<ValidationEvent> eventos = new ArrayList<>();
        List<ValidationQuarantine> quarentena = new ArrayList<>();
        List<NgsiLdEntityDto> entidadesNgsiLd = new ArrayList<>();
        int totalLinhas = 0;
        int duplicadosDescartados = 0;

        Set<String> hashesPacote = new HashSet<>();
        List<HeaderInfo> headerInfos;

        String line;
        line = reader.readLine();
        if (line == null) {
            return new CsvImportResultDto(0, 0, 0, 0, 0, 0, 0, etlMappingVersion, null, true, "EMPTY", List.of());
        }
        headerInfos = parseHeaders(line);
        auditarCamposNaoMapeados(headerInfos);

        while ((line = reader.readLine()) != null) {
            totalLinhas++;
            String[] parts = line.split(",", -1);
            if (parts.length < 14) {
                quarentena.add(new ValidationQuarantine(
                    line,
                    "Colunas insuficientes",
                    "payload",
                    "n/a",
                    "Campos obrigatorios ausentes no schema",
                    OffsetDateTime.now()
                ));
                continue;
            }

            ValidationResult validacao = validarLinha(parts);
            if (!validacao.isValid()) {
                quarentena.add(new ValidationQuarantine(
                    line,
                    validacao.getReason(),
                    validacao.getField(),
                    validacao.getReceivedValue(),
                    validacao.getRule(),
                    OffsetDateTime.now()
                ));
                continue;
            }

            String ingestionHash = buildIngestionHash(parts);
            if (hashesPacote.contains(ingestionHash)) {
                duplicadosDescartados++;
                auditar("DUPLICATE_INTRA_PACKAGE", "ingestionHash", ingestionHash, "Duplicado no mesmo pacote CSV");
                continue;
            }
            hashesPacote.add(ingestionHash);

            OffsetDateTime agora = OffsetDateTime.now();
            OffsetDateTime janela = agora.minus(JANELA_DUPLICADOS);
            if (validationEventRepository.existsByIngestionHashAndIngestedAtAfter(ingestionHash, janela)) {
                duplicadosDescartados++;
                auditar("DUPLICATE_INTER_PACKAGE", "ingestionHash", ingestionHash, "Duplicado nas ultimas 24h");
                continue;
            }

            ParseLinhaResult parseResult = parseLinha(line, parts, ingestionHash, headerInfos);
            if (parseResult.evento().isPresent()) {
                ValidationEvent evento = parseResult.evento().get();
                eventos.add(evento);
                entidadesNgsiLd.add(toNgsiLdEntity(evento));
            } else if (parseResult.quarentena() != null) {
                quarentena.add(parseResult.quarentena());
            } else {
                quarentena.add(new ValidationQuarantine(
                    line,
                    "Erro na normalizacao",
                    "payload",
                    "n/a",
                    "Falha no mapeamento SCB -> SmartDataModels",
                    OffsetDateTime.now()
                ));
            }
        }

        long fimValidacao = System.currentTimeMillis();
        validationEventRepository.saveAll(eventos);
        validationQuarantineRepository.saveAll(quarentena);

        if (!quarentena.isEmpty()) {
            notificarGestorOperacoes("Existem " + quarentena.size() + " registos em quarentena.");
        }

        long inicioPersistencia = System.currentTimeMillis();
        PersistResult persistResult = persistirUc023(entidadesNgsiLd);
        long fimTotal = System.currentTimeMillis();
        long tempoValidacaoMs = fimValidacao - inicioValidacao;
        long tempoNormalizacaoMs = inicioPersistencia - fimValidacao;
        long tempoPersistenciaMs = fimTotal - inicioPersistencia;

        if (tempoValidacaoMs > LIMITE_VALIDACAO_MS) {
            auditar("SLA_WARNING", "validacao", String.valueOf(tempoValidacaoMs), "Validacao excedeu 5 segundos");
        }
        if (tempoNormalizacaoMs > LIMITE_NORMALIZACAO_MS) {
            auditar("SLA_WARNING", "normalizacao", String.valueOf(tempoNormalizacaoMs), "Normalizacao excedeu 10 segundos");
        }
        if (tempoPersistenciaMs > LIMITE_PERSISTENCIA_MS) {
            auditar("SLA_WARNING", "persistencia", String.valueOf(tempoPersistenciaMs), "Persistencia excedeu 15 segundos");
        }

        auditar("BACKUP_POLICY_INFO", "persistencia", "mysql-managed", "Dados persistidos sujeitos a politica de backup da base de dados");
        auditar("INGESTION_SUCCESS", "batchId", persistResult.batchId(), "Ingestao concluida com sucesso");

        return new CsvImportResultDto(
            totalLinhas,
            eventos.size(),
            quarentena.size(),
            duplicadosDescartados,
            tempoValidacaoMs,
            tempoNormalizacaoMs,
            tempoPersistenciaMs,
            etlMappingVersion,
            persistResult.batchId(),
            persistResult.persistenciaConfirmada(),
            persistResult.estadoLote(),
            entidadesNgsiLd
        );
    }

    private BufferedReader getClasspathCsvReader() {
        InputStream is = getClass().getClassLoader().getResourceAsStream(csvFileName);
        if (is == null) {
            throw new IllegalStateException("CSV nao encontrado no classpath: " + csvFileName);
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private ParseLinhaResult parseLinha(String rawLine, String[] parts, String ingestionHash, List<HeaderInfo> headers) {
        try {
            String cardId = valorOuNull(parts[0]);
            String ticketId = valorOuNull(parts[1]);
            String mediaType = valorOuNull(parts[2]);
            String ticketTypeCode = canonicalTicketType(valorOuNull(parts[3]));
            String transactionType = valorOuNull(parts[4]);
            String transactionDateTime = valorOuNull(parts[5]);
            String originStopId = valorOuNull(parts[6]);
            String routeId = valorOuNull(parts[7]);
            String tripId = valorOuNull(parts[8]);
            String fareForAdult = valorOuNull(parts[9]);
            String equipmentId = valorOuNull(parts[10]);
            String transactionVehicleNum = valorOuNull(parts[11]);
            String result = valorOuNull(parts[12]);
            String rejectReason = valorOuNull(parts[13]);

            if (mediaType == null || ticketTypeCode == null || transactionType == null || transactionDateTime == null || result == null) {
                return new ParseLinhaResult(Optional.empty(), null);
            }

            TicketType ticketType = ticketTypeRepository.findByCode(ticketTypeCode)
                .orElseGet(() -> ticketTypeRepository.save(new TicketType(ticketTypeCode, ticketTypeCode)));

            Stop stop = null;
            if (originStopId != null) {
                stop = stopRepository.findById(originStopId).orElse(null);
            }

            FareCollectionSystem fareCollectionSystem = resolveFareCollectionSystem(equipmentId, transactionVehicleNum);

            ValidationEvent evento = new ValidationEvent();
            evento.setCardId(pseudonimizarIdentificador(cardId));
            evento.setTicketId(ticketId);
            evento.setIngestionHash(ingestionHash);
            evento.setIngestedAt(OffsetDateTime.now());
            evento.setMediaType(mediaType);
            evento.setTicketType(ticketType);
            evento.setTransactionType(transactionType);
            evento.setTransactionDateTime(OffsetDateTime.parse(transactionDateTime));
            evento.setOriginStop(stop);
            evento.setRouteId(routeId);
            evento.setTripId(tripId);
            evento.setFareCollectionSystem(fareCollectionSystem);
            try {
                evento.setFareForAdult(fareForAdult == null ? null : new BigDecimal(fareForAdult));
            } catch (NumberFormatException e) {
                return new ParseLinhaResult(
                    Optional.empty(),
                    new ValidationQuarantine(
                        rawLine,
                        "fareForAdult invalido",
                        "fareForAdult",
                        fareForAdult,
                        "Tipo de dado invalido (decimal)",
                        OffsetDateTime.now()
                    )
                );
            }
            evento.setEquipmentId(equipmentId);
            evento.setTransactionVehicleNum(transactionVehicleNum);
            evento.setResult(result);
            evento.setRejectReason(rejectReason);


            auditarCamposNaoMapeadosComValor(headers, parts);

            return new ParseLinhaResult(Optional.of(evento), null);
        } catch (Exception e) {
            return new ParseLinhaResult(Optional.empty(), null);
        }
    }

    private String valorOuNull(String valor) {
        if (valor == null) {
            return null;
        }
        String tratado = valor.trim();
        return tratado.isEmpty() ? null : tratado;
    }

    private FareCollectionSystem resolveFareCollectionSystem(String equipmentId, String vehicleNum) {
        String systemCode = buildSystemCode(equipmentId, vehicleNum);
        return fareCollectionSystemRepository.findBySystemCode(systemCode)
            .orElseGet(() -> fareCollectionSystemRepository.save(new FareCollectionSystem(systemCode, equipmentId, vehicleNum)));
    }

    private String buildSystemCode(String equipmentId, String vehicleNum) {
        String equipment = equipmentId == null ? "SEM_EQUIPAMENTO" : equipmentId;
        String vehicle = vehicleNum == null ? "SEM_VEICULO" : vehicleNum;
        return equipment + "@" + vehicle;
    }

    private ValidationResult validarLinha(String[] parts) {
        String transactionDateTime = valorOuNull(parts[5]);
        String originStopId = valorOuNull(parts[6]);
        String routeId = valorOuNull(parts[7]);
        String tripId = valorOuNull(parts[8]);
        String ticketType = valorOuNull(parts[3]);

        if (transactionDateTime == null) {
            return ValidationResult.invalid("transactionDateTime em falta", "transactionDateTime", null, "Campo obrigatorio");
        }
        if (routeId == null) {
            return ValidationResult.invalid("ID da linha em falta", "route_id", null, "Campo obrigatorio");
        }
        if (ticketType == null) {
            return ValidationResult.invalid("ticketTypeCode em falta", "ticketTypeCode", null, "Campo obrigatorio");
        }
        if (tripId == null) {
            return ValidationResult.invalid("trip_id em falta", "trip_id", null, "Campo obrigatorio");
        }

        OffsetDateTime ts;
        try {
            ts = OffsetDateTime.parse(transactionDateTime);
        } catch (Exception e) {
            return ValidationResult.invalid("transactionDateTime invalido", "transactionDateTime", transactionDateTime, "Tipo de dado invalido");
        }

        if (canonicalTicketType(ticketType) == null) {
            return ValidationResult.invalid(
                "ticketTypeCode fora das categorias validas",
                "ticketTypeCode",
                ticketType,
                "Valores validos: passe estudante, passe senior, mensal, avulso"
            );
        }

        if (!existeLinhaNoCatalogo(routeId)) {
            return ValidationResult.invalid(
                "route_id nao encontrado no catalogo",
                "route_id",
                routeId,
                "Integridade referencial"
            );
        }

        Optional<com.example.demo.model.Trip> trip = tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            return ValidationResult.invalid(
                "trip_id nao encontrado no catalogo",
                "trip_id",
                tripId,
                "Integridade referencial"
            );
        }

        if (!routeId.equals(trip.get().getRouteId())) {
            return ValidationResult.invalid(
                "trip_id nao pertence ao route_id indicado",
                "trip_id",
                tripId,
                "Coerencia relacional route-trip"
            );
        }

        if (originStopId != null) {
            if (!stopRepository.existsById(originStopId)) {
                return ValidationResult.invalid(
                    "originStopId nao encontrado no catalogo",
                    "originStopId",
                    originStopId,
                    "Integridade referencial"
                );
            }
            if (!stopTimesRepository.existsByTripIdAndStopId(tripId, originStopId)) {
                return ValidationResult.invalid(
                    "originStopId nao pertence ao trip_id indicado",
                    "originStopId",
                    originStopId,
                    "Coerencia relacional trip-stop"
                );
            }
        }

        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime limitePassado = agora.minus(LIMITE_PASSADO);
        OffsetDateTime limiteFuturo = agora.plus(TOLERANCIA_FUTURO);

        if (ts.isBefore(limitePassado) || ts.isAfter(limiteFuturo)) {
            Duration desvio = ts.isBefore(limitePassado)
                ? Duration.between(ts, limitePassado)
                : Duration.between(limiteFuturo, ts);
            return ValidationResult.invalid(
                "timestamp fora da janela temporal",
                "transactionDateTime",
                transactionDateTime,
                "Desvio temporal: " + desvio.toMinutes() + " minutos"
            );
        }

        return ValidationResult.valid();
    }

    private String buildIngestionHash(String[] parts) {
        String payload = String.join("|", parts);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    private String pseudonimizarIdentificador(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(pseudonymSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);
            byte[] bytes = hmac.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return buildIngestionHash(new String[]{valor});
        }
    }

    private String canonicalTicketType(String raw) {
        if (raw == null) {
            return null;
        }
        return TICKET_TYPES_CANONICOS.get(raw.trim().toUpperCase());
    }

    private boolean existeLinhaNoCatalogo(String routeId) {
        try {
            Long numericId = Long.parseLong(routeId);
            return routeRepository.existsById(numericId);
        } catch (NumberFormatException ignored) {
            return routeRepository.existsByRouteShortName(routeId);
        }
    }

    private List<HeaderInfo> parseHeaders(String headerLine) {
        String[] headers = headerLine.split(",", -1);
        List<HeaderInfo> info = new ArrayList<>();
        for (int i = 0; i < headers.length; i++) {
            String nome = headers[i].trim();
            info.add(new HeaderInfo(i, nome, SCB_HEADERS_ESPERADOS.contains(nome)));
        }
        return info;
    }

    private void auditarCamposNaoMapeados(List<HeaderInfo> headerInfos) {
        List<HeaderInfo> naoMapeados = headerInfos.stream().filter(h -> !h.mapeado()).collect(Collectors.toList());
        for (HeaderInfo header : naoMapeados) {
            auditar("UNMAPPED_FIELD", header.nome(), null, "Campo sem correspondencia SCB -> SmartDataModels");
            notificarGestorOperacoes("Campo nao mapeado: " + header.nome());
        }
    }

    private void auditarCamposNaoMapeadosComValor(List<HeaderInfo> headerInfos, String[] parts) {
        for (HeaderInfo header : headerInfos) {
            if (header.mapeado()) {
                continue;
            }
            if (header.index() >= parts.length) {
                continue;
            }
            String valor = valorOuNull(parts[header.index()]);
            if (valor != null) {
                auditar("UNMAPPED_VALUE", header.nome(), valor, "Valor de campo nao mapeado mantido apenas em auditoria");
                notificarGestorOperacoes("Valor em campo nao mapeado " + header.nome() + ": " + valor);
            }
        }
    }

    private void auditar(String eventType, String fieldName, String originalValue, String detail) {
        ingestionAuditLogRepository.save(new IngestionAuditLog(eventType, fieldName, originalValue, detail, OffsetDateTime.now()));
    }

    private void notificarGestorOperacoes(String mensagem) {
        LOG.warn("NOTIFICACAO_GESTOR_TUB: {}", mensagem);
    }

    private NgsiLdEntityDto toNgsiLdEntity(ValidationEvent evento) {
        Map<String, Object> props = new HashMap<>();
        props.put("transactionDateTime", ngsiProperty(evento.getTransactionDateTime().toString()));
        props.put("transactionType", ngsiProperty(evento.getTransactionType()));
        props.put("ticketTypeCode", ngsiProperty(evento.getTicketType().getCode()));
        props.put("originStop", ngsiRelationship(buildStopUrn(evento.getOriginStop())));
        props.put("route", ngsiRelationship(buildRouteUrn(evento.getRouteId())));
        props.put("trip", ngsiRelationship(buildTripUrn(evento.getTripId())));
        props.put("equipmentId", ngsiProperty(evento.getEquipmentId()));
        props.put("transactionVehicleNum", ngsiProperty(evento.getTransactionVehicleNum()));
        props.put("fareForAdult", ngsiProperty(evento.getFareForAdult()));
        props.put("result", ngsiProperty(evento.getResult()));
        props.put("cardId", ngsiProperty(evento.getCardId()));
        if (evento.getOriginStop() != null && evento.getOriginStop().getStopLat() != null && evento.getOriginStop().getStopLon() != null) {
            props.put("location", ngsiGeoProperty(evento.getOriginStop().getStopLat(), evento.getOriginStop().getStopLon()));
        }

        String ngsiId = "urn:ngsi-ld:FareCollectionSystem:" + evento.getIngestionHash();
        return new NgsiLdEntityDto(ngsiId, "FareCollectionSystem", etlMappingVersion, props);
    }

    private Map<String, Object> ngsiProperty(Object value) {
        Map<String, Object> prop = new HashMap<>();
        prop.put("type", "Property");
        prop.put("value", value);
        return prop;
    }

    private Map<String, Object> ngsiRelationship(String objectUrn) {
        Map<String, Object> rel = new HashMap<>();
        rel.put("type", "Relationship");
        rel.put("object", objectUrn);
        return rel;
    }

    private Map<String, Object> ngsiGeoProperty(Double lat, Double lon) {
        Map<String, Object> geo = new HashMap<>();
        geo.put("type", "GeoProperty");
        Map<String, Object> value = new HashMap<>();
        value.put("type", "Point");
        value.put("coordinates", List.of(lon, lat));
        geo.put("value", value);
        return geo;
    }

    private String buildStopUrn(Stop stop) {
        if (stop == null || stop.getStopId() == null) {
            return null;
        }
        return "urn:ngsi-ld:Stop:" + stop.getStopId();
    }

    private String buildRouteUrn(String routeId) {
        if (routeId == null || routeId.isBlank()) {
            return null;
        }
        return "urn:ngsi-ld:Route:" + routeId;
    }

    private String buildTripUrn(String tripId) {
        if (tripId == null || tripId.isBlank()) {
            return null;
        }
        return "urn:ngsi-ld:Trip:" + tripId;
    }

    private PersistResult persistirUc023(List<NgsiLdEntityDto> entidadesNgsiLd) {
        String batchId = UUID.randomUUID().toString();
        return persistirUc023(entidadesNgsiLd, batchId, false);
    }

    private PersistResult persistirUc023(List<NgsiLdEntityDto> entidadesNgsiLd, String batchId, boolean fromRetry) {
        LocalDate particao = LocalDate.now();
        OffsetDateTime agora = OffsetDateTime.now();
        long inicio = System.currentTimeMillis();

        List<NgsiLdDataLakeRecord> records = entidadesNgsiLd.stream()
            .map(entity -> new NgsiLdDataLakeRecord(
                batchId,
                entity.getId(),
                entity.getType(),
                toJson(entity),
                particao,
                parseObservationDateTime(entity),
                extractLat(entity),
                extractLon(entity),
                agora
            ))
            .collect(Collectors.toList());

        long persisted;
        long expected = records.size();
        long tempoPersistencia;

        try {
            ngsiLdDataLakeRecordRepository.saveAll(records);
            persisted = ngsiLdDataLakeRecordRepository.countByBatchId(batchId);
            tempoPersistencia = System.currentTimeMillis() - inicio;
        } catch (Exception e) {
            tempoPersistencia = System.currentTimeMillis() - inicio;
            if (!fromRetry) {
                enqueueRetry(batchId, "Erro na escrita inicial do Data Lake: " + e.getMessage(), entidadesNgsiLd);
            }

            ingestionBatchControlRepository.save(new IngestionBatchControl(
                batchId,
                agora,
                (int) expected,
                0,
                etlMappingVersion,
                particao,
                "RETRY_PENDING",
                tempoPersistencia
            ));

            notificarGestorOperacoes("Falha na persistencia do lote " + batchId + ". Agendado retry.");
            return new PersistResult(batchId, false, "RETRY_PENDING");
        }

        boolean ok = persisted == expected;
        String estado = ok ? "SUCCESS" : "RETRY_PENDING";

        ingestionBatchControlRepository.save(new IngestionBatchControl(
            batchId,
            agora,
            (int) expected,
            (int) persisted,
            etlMappingVersion,
            particao,
            estado,
            tempoPersistencia
        ));

        if (!ok) {
            String reason = "Integridade pos-escrita falhou. Esperado=" + expected + ", Persistido=" + persisted;
            if (!fromRetry) {
                enqueueRetry(batchId, reason, entidadesNgsiLd);
            }
            notificarGestorOperacoes("Lote " + batchId + " movido para retry queue. " + reason);
        }

        return new PersistResult(batchId, ok, estado);
    }

    private String toJson(NgsiLdEntityDto entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha a serializar entidade NGSI-LD", e);
        }
    }

    public boolean reprocessarLoteRetry(IngestionRetryQueue retryQueue) {
        try {
            List<NgsiLdEntityDto> entidades = objectMapper.readValue(
                retryQueue.getPayloadJson(),
                new TypeReference<List<NgsiLdEntityDto>>() {
                }
            );
            PersistResult result = persistirUc023(entidades, retryQueue.getBatchId(), true);
            if (result.persistenciaConfirmada()) {
                auditar("RETRY_SUCCESS", "batchId", retryQueue.getBatchId(), "Lote persistido com sucesso apos retry");
            }
            return result.persistenciaConfirmada();
        } catch (Exception e) {
            auditar("RETRY_ERROR", "batchId", retryQueue.getBatchId(), "Falha no retry: " + e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private OffsetDateTime parseObservationDateTime(NgsiLdEntityDto entity) {
        try {
            Object raw = entity.getProperties().get("transactionDateTime");
            if (!(raw instanceof Map)) {
                return null;
            }
            Object value = ((Map<String, Object>) raw).get("value");
            if (value == null) {
                return null;
            }
            return OffsetDateTime.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Double extractLat(NgsiLdEntityDto entity) {
        return extractCoordinates(entity, 1);
    }

    private Double extractLon(NgsiLdEntityDto entity) {
        return extractCoordinates(entity, 0);
    }

    @SuppressWarnings("unchecked")
    private Double extractCoordinates(NgsiLdEntityDto entity, int index) {
        try {
            Object raw = entity.getProperties().get("location");
            if (!(raw instanceof Map<?, ?> locationMap)) {
                return null;
            }
            Object valueRaw = locationMap.get("value");
            if (!(valueRaw instanceof Map<?, ?> valueMap)) {
                return null;
            }
            Object coordinatesRaw = valueMap.get("coordinates");
            if (!(coordinatesRaw instanceof List<?> coordinates) || coordinates.size() < 2) {
                return null;
            }
            Object coordinate = coordinates.get(index);
            if (coordinate == null) {
                return null;
            }
            return Double.parseDouble(coordinate.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void enqueueRetry(String batchId, String reason, List<NgsiLdEntityDto> entidadesNgsiLd) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(entidadesNgsiLd);
        } catch (JsonProcessingException e) {
            payload = "[]";
        }

        OffsetDateTime agora = OffsetDateTime.now();
        ingestionRetryQueueRepository.save(new IngestionRetryQueue(
            batchId,
            reason,
            "PENDING",
            0,
            3,
            agora.plus(Duration.ofMillis(retryIntervalMs)),
            null,
            payload,
            agora
        ));
    }

    private static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final String field;
        private final String receivedValue;
        private final String rule;

        private ValidationResult(boolean valid, String reason, String field, String receivedValue, String rule) {
            this.valid = valid;
            this.reason = reason;
            this.field = field;
            this.receivedValue = receivedValue;
            this.rule = rule;
        }

        private static ValidationResult valid() {
            return new ValidationResult(true, null, null, null, null);
        }

        private static ValidationResult invalid(String reason, String field, String receivedValue, String rule) {
            return new ValidationResult(false, reason, field, receivedValue, rule);
        }

        private boolean isValid() {
            return valid;
        }

        private String getReason() {
            return reason;
        }

        private String getField() {
            return field;
        }

        private String getReceivedValue() {
            return receivedValue;
        }

        private String getRule() {
            return rule;
        }
    }

    private record HeaderInfo(int index, String nome, boolean mapeado) {
    }

    private record ParseLinhaResult(Optional<ValidationEvent> evento, ValidationQuarantine quarentena) {
    }

    private record PersistResult(String batchId, boolean persistenciaConfirmada, String estadoLote) {
    }
}