package pt.tub.ticketub.p2_ingestao_processamento_dados;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationIngestionRequestDto;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationIngestionResponseDto;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.FareCollectionSystem;
import pt.tub.ticketub.p2_ingestao_processamento_dados.IngestionAuditLog;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Stop;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TicketType;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantine;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.FareCollectionSystemRepository;
import pt.tub.ticketub.p2_ingestao_processamento_dados.IngestionAuditLogRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RouteRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopTimesRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TicketTypeRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TripRepository;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantineRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.NgsiLdEntityDto;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.NgsiLdDataLakeRecord;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.NgsiLdDataLakeRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ValidationIngestionService {

    private static final Duration JANELA_DUPLICADOS = Duration.ofHours(24);
    private static final Duration LIMITE_PASSADO = Duration.ofDays(90);
    private static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.ingestion.pseudonym.secret:dev-secret-change-me}")
    private String pseudonymSecret;

    @Value("${app.etl.mapping-version:v1.0.0}")
    private String etlMappingVersion;

    public ValidationIngestionService(
        StopRepository stopRepository,
        RouteRepository routeRepository,
        TripRepository tripRepository,
        StopTimesRepository stopTimesRepository,
        TicketTypeRepository ticketTypeRepository,
        ValidationEventRepository validationEventRepository,
        FareCollectionSystemRepository fareCollectionSystemRepository,
        ValidationQuarantineRepository validationQuarantineRepository,
        IngestionAuditLogRepository ingestionAuditLogRepository,
        NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository
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
    }

    @Transactional
    public ValidationIngestionResponseDto ingerir(List<ValidationIngestionRequestDto> validacoes) {
        if (validacoes == null || validacoes.isEmpty()) {
            return new ValidationIngestionResponseDto(0, 0, 0, 0);
        }

        List<ValidationEvent> eventos = new ArrayList<>();
        List<ValidationQuarantine> quarentena = new ArrayList<>();
        Set<String> hashesPacote = new HashSet<>();
        int duplicadosDescartados = 0;

        for (ValidationIngestionRequestDto dto : validacoes) {
            if (dto == null) {
                quarentena.add(new ValidationQuarantine(
                    "{}",
                    "Payload nulo",
                    "payload",
                    null,
                    "Objeto de validacao obrigatorio",
                    OffsetDateTime.now()
                ));
                continue;
            }

            ValidationResult validacao = validarPayload(dto);
            if (!validacao.isValid()) {
                quarentena.add(toQuarentena(dto, validacao));
                continue;
            }

            String ingestionHash = buildIngestionHash(dto);
            if (hashesPacote.contains(ingestionHash)) {
                duplicadosDescartados++;
                auditar("DUPLICATE_INTRA_PACKAGE", "ingestionHash", ingestionHash, "Duplicado no mesmo payload");
                continue;
            }
            hashesPacote.add(ingestionHash);

            OffsetDateTime janela = OffsetDateTime.now().minus(JANELA_DUPLICADOS);
            if (validationEventRepository.existsByIngestionHashAndIngestedAtAfter(ingestionHash, janela)) {
                duplicadosDescartados++;
                auditar("DUPLICATE_INTER_PACKAGE", "ingestionHash", ingestionHash, "Duplicado nas ultimas 24h");
                continue;
            }

            try {
                eventos.add(toEvento(dto, ingestionHash));
            } catch (Exception e) {
                quarentena.add(new ValidationQuarantine(
                    toJson(dto),
                    "Falha na normalizacao",
                    "payload",
                    null,
                    e.getMessage(),
                    OffsetDateTime.now()
                ));
            }
        }

        validationEventRepository.saveAll(eventos);
        validationQuarantineRepository.saveAll(quarentena);

        // Persistir eventos válidos como entidades NGSI-LD no Data Lake
        persistirNgsiLdDataLake(eventos);

        return new ValidationIngestionResponseDto(
            validacoes.size(),
            eventos.size(),
            quarentena.size(),
            duplicadosDescartados
        );
    }

    private ValidationEvent toEvento(ValidationIngestionRequestDto dto, String ingestionHash) {
        String mediaType = valorOuNull(dto.getMediaType());
        String ticketTypeCode = canonicalTicketType(valorOuNull(dto.getTicketTypeCode()));
        String transactionType = valorOuNull(dto.getTransactionType());
        String transactionDateTime = valorOuNull(dto.getTransactionDateTime());
        String originStopId = valorOuNull(dto.getOriginStopId());
        String routeId = valorOuNull(dto.getRouteId());
        String tripId = valorOuNull(dto.getTripId());
        String fareForAdult = valorOuNull(dto.getFareForAdult());
        String equipmentId = valorOuNull(dto.getEquipmentId());
        String transactionVehicleNum = valorOuNull(dto.getTransactionVehicleNum());
        String result = valorOuNull(dto.getResult());
        String rejectReason = valorOuNull(dto.getRejectReason());

        TicketType ticketType = ticketTypeRepository.findByCode(ticketTypeCode)
            .orElseGet(() -> ticketTypeRepository.save(new TicketType(ticketTypeCode, ticketTypeCode)));

        Stop stop = null;
        if (originStopId != null) {
            stop = stopRepository.findById(originStopId).orElse(null);
        }

        FareCollectionSystem fareCollectionSystem = resolveFareCollectionSystem(equipmentId, transactionVehicleNum);

        ValidationEvent evento = new ValidationEvent();
        evento.setCardId(pseudonimizarIdentificador(valorOuNull(dto.getCardId())));
        evento.setTicketId(valorOuNull(dto.getTicketId()));
        evento.setIngestionHash(ingestionHash);
        evento.setIngestedAt(OffsetDateTime.now());
        evento.setMediaType(mediaType == null ? "NFC_SMARTCARD" : mediaType);
        evento.setTicketType(ticketType);
        evento.setTransactionType(transactionType == null ? "VALIDATION" : transactionType);
        evento.setTransactionDateTime(OffsetDateTime.parse(transactionDateTime));
        evento.setOriginStop(stop);
        evento.setRouteId(routeId);
        evento.setTripId(tripId);
        evento.setFareCollectionSystem(fareCollectionSystem);
        evento.setFareForAdult(fareForAdult == null ? null : new BigDecimal(fareForAdult));
        evento.setEquipmentId(equipmentId);
        evento.setTransactionVehicleNum(transactionVehicleNum);
        evento.setResult(result);
        evento.setRejectReason(rejectReason);

        return evento;
    }

    private ValidationResult validarPayload(ValidationIngestionRequestDto dto) {
        String transactionDateTime = valorOuNull(dto.getTransactionDateTime());
        String originStopId = valorOuNull(dto.getOriginStopId());
        String routeId = valorOuNull(dto.getRouteId());
        String tripId = valorOuNull(dto.getTripId());
        String ticketType = valorOuNull(dto.getTicketTypeCode());
        String result = valorOuNull(dto.getResult());
        String fareForAdult = valorOuNull(dto.getFareForAdult());

        if (transactionDateTime == null) {
            return ValidationResult.invalid("transactionDateTime em falta", "transactionDateTime", null, "Campo obrigatorio");
        }
        if (routeId == null) {
            return ValidationResult.invalid("route_id em falta", "route_id", null, "Campo obrigatorio");
        }
        if (ticketType == null) {
            return ValidationResult.invalid("ticketTypeCode em falta", "ticketTypeCode", null, "Campo obrigatorio");
        }
        if (tripId == null) {
            return ValidationResult.invalid("trip_id em falta", "trip_id", null, "Campo obrigatorio");
        }
        if (result == null) {
            return ValidationResult.invalid("result em falta", "result", null, "Campo obrigatorio");
        }

        OffsetDateTime ts;
        try {
            ts = OffsetDateTime.parse(transactionDateTime);
        } catch (Exception e) {
            return ValidationResult.invalid("transactionDateTime invalido", "transactionDateTime", transactionDateTime, "Formato ISO-8601");
        }

        if (fareForAdult != null) {
            try {
                new BigDecimal(fareForAdult);
            } catch (NumberFormatException e) {
                return ValidationResult.invalid("fareForAdult invalido", "fareForAdult", fareForAdult, "Tipo decimal esperado");
            }
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
            return ValidationResult.invalid("route_id nao encontrado", "route_id", routeId, "Integridade referencial");
        }

        Optional<pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Trip> trip = tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            return ValidationResult.invalid("trip_id nao encontrado", "trip_id", tripId, "Integridade referencial");
        }

        if (!routeId.equals(trip.get().getRouteId())) {
            return ValidationResult.invalid("trip_id nao pertence ao route_id", "trip_id", tripId, "Coerencia route-trip");
        }

        if (originStopId != null) {
            if (!stopRepository.existsById(originStopId)) {
                return ValidationResult.invalid("originStopId nao encontrado", "originStopId", originStopId, "Integridade referencial");
            }
            if (!stopTimesRepository.existsByTripIdAndStopId(tripId, originStopId)) {
                return ValidationResult.invalid("originStopId nao pertence ao trip_id", "originStopId", originStopId, "Coerencia trip-stop");
            }
        }

        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime limitePassado = agora.minus(LIMITE_PASSADO);
        OffsetDateTime limiteFuturo = agora.plus(TOLERANCIA_FUTURO);

        if (ts.isBefore(limitePassado) || ts.isAfter(limiteFuturo)) {
            return ValidationResult.invalid("timestamp fora da janela temporal", "transactionDateTime", transactionDateTime, "Janela: -90 dias e +5 minutos");
        }

        return ValidationResult.valid();
    }

    private ValidationQuarantine toQuarentena(ValidationIngestionRequestDto dto, ValidationResult validacao) {
        return new ValidationQuarantine(
            toJson(dto),
            validacao.getReason(),
            validacao.getField(),
            validacao.getReceivedValue(),
            validacao.getRule(),
            OffsetDateTime.now()
        );
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

    private String valorOuNull(String valor) {
        if (valor == null) {
            return null;
        }
        String tratado = valor.trim();
        return tratado.isEmpty() ? null : tratado;
    }

    private String valorOuVazio(String valor) {
        String tratado = valorOuNull(valor);
        return tratado == null ? "" : tratado;
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

    private String buildIngestionHash(ValidationIngestionRequestDto dto) {
        String payload = String.join("|",
            valorOuVazio(dto.getCardId()),
            valorOuVazio(dto.getTicketId()),
            valorOuVazio(dto.getMediaType()),
            valorOuVazio(dto.getTicketTypeCode()),
            valorOuVazio(dto.getTransactionType()),
            valorOuVazio(dto.getTransactionDateTime()),
            valorOuVazio(dto.getOriginStopId()),
            valorOuVazio(dto.getRouteId()),
            valorOuVazio(dto.getTripId()),
            valorOuVazio(dto.getFareForAdult()),
            valorOuVazio(dto.getEquipmentId()),
            valorOuVazio(dto.getTransactionVehicleNum()),
            valorOuVazio(dto.getResult()),
            valorOuVazio(dto.getRejectReason())
        );

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
            return Integer.toHexString(valor.hashCode());
        }
    }

    private String toJson(ValidationIngestionRequestDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void persistirNgsiLdDataLake(List<ValidationEvent> eventos) {
        if (eventos == null || eventos.isEmpty()) {
            return;
        }

        String batchId = UUID.randomUUID().toString();
        LocalDate particao = LocalDate.now();
        OffsetDateTime agora = OffsetDateTime.now();

        List<NgsiLdDataLakeRecord> records = new ArrayList<>();
        for (ValidationEvent evento : eventos) {
            try {
                NgsiLdEntityDto entidade = toNgsiLdEntity(evento);
                NgsiLdDataLakeRecord record = new NgsiLdDataLakeRecord(
                    batchId,
                    entidade.getId(),
                    entidade.getType(),
                    toJson(entidade),
                    particao,
                    evento.getTransactionDateTime(),
                    evento.getOriginStop() != null ? evento.getOriginStop().getStopLat() : null,
                    evento.getOriginStop() != null ? evento.getOriginStop().getStopLon() : null,
                    agora
                );
                records.add(record);
            } catch (Exception e) {
                auditar("NGSI_LD_CONVERSION_ERROR", evento.getIngestionHash(), e.getMessage(), "Falha na conversão NGSI-LD");
            }
        }

        if (!records.isEmpty()) {
            try {
                ngsiLdDataLakeRecordRepository.saveAll(records);
                auditar("NGSI_LD_PERSISTENCE_SUCCESS", "batchId", batchId, "Eventos persistidos no Data Lake");
            } catch (Exception e) {
                auditar("NGSI_LD_PERSISTENCE_ERROR", "batchId", batchId, "Erro na persistência NGSI-LD: " + e.getMessage());
            }
        }
    }

    private NgsiLdEntityDto toNgsiLdEntity(ValidationEvent evento) {
        Map<String, Object> props = new HashMap<>();

        // Propriedades de dados
        props.put("transactionDateTime", ngsiProperty(evento.getTransactionDateTime().toString()));
        props.put("transactionType", ngsiProperty(evento.getTransactionType()));
        props.put("ticketTypeCode", ngsiProperty(evento.getTicketType().getCode()));
        props.put("mediaType", ngsiProperty(evento.getMediaType()));
        props.put("result", ngsiProperty(evento.getResult()));
        props.put("equipmentId", ngsiProperty(evento.getEquipmentId()));
        props.put("transactionVehicleNum", ngsiProperty(evento.getTransactionVehicleNum()));
        props.put("cardId", ngsiProperty(evento.getCardId()));

        if (evento.getFareForAdult() != null) {
            props.put("fareForAdult", ngsiProperty(evento.getFareForAdult().doubleValue()));
        }

        if (evento.getRejectReason() != null) {
            props.put("rejectReason", ngsiProperty(evento.getRejectReason()));
        }

        // Relações a outras entidades
        if (evento.getOriginStop() != null) {
            props.put("originStop", ngsiRelationship("urn:ngsi-ld:Stop:" + evento.getOriginStop().getStopId()));
        }

        if (evento.getRouteId() != null) {
            props.put("route", ngsiRelationship("urn:ngsi-ld:Route:" + evento.getRouteId()));
        }

        if (evento.getTripId() != null) {
            props.put("trip", ngsiRelationship("urn:ngsi-ld:Trip:" + evento.getTripId()));
        }

        // Geolocalização
        if (evento.getOriginStop() != null && evento.getOriginStop().getStopLat() != null && evento.getOriginStop().getStopLon() != null) {
            props.put("location", ngsiGeoProperty(evento.getOriginStop().getStopLat(), evento.getOriginStop().getStopLon()));
        }

        String ngsiId = "urn:ngsi-ld:FareTransaction:" + evento.getIngestionHash();
        return new NgsiLdEntityDto(ngsiId, "FareTransaction", etlMappingVersion, props);
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

    private String toJson(NgsiLdEntityDto entidade) {
        try {
            return objectMapper.writeValueAsString(entidade);
        } catch (Exception e) {
            throw new IllegalStateException("Falha a serializar entidade NGSI-LD", e);
        }
    }

    private void auditar(String eventType, String fieldName, String originalValue, String detail) {
        ingestionAuditLogRepository.save(new IngestionAuditLog(eventType, fieldName, originalValue, detail, OffsetDateTime.now()));
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
}






