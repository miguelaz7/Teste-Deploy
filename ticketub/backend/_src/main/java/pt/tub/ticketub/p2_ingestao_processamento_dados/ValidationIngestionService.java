package pt.tub.ticketub.p2_ingestao_processamento_dados;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.FareCollectionSystem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Stop;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TicketType;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantine;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.FareCollectionSystemRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RouteRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopTimesRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TicketTypeRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TripRepository;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * O0.2.1.c – Controlador de Validação de Picagens
 * O0.2.2.c – Controlador de Normalização e Anonimização
 * O0.2.3.c – Controlador de Persistência no Data Lake
 * O0.2.4.c – Controlador de Registo de Estatísticas e Auditoria
 */
@Service
public class ValidationIngestionService {

    private static final Duration JANELA_DUPLICADOS = Duration.ofHours(24);
    private static final double LIMIAR_QUALIDADE = 0.05;

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
    // O0.2.1.d – Repositório de Regras de Validação
    private final ValidationRuleRepository validationRuleRepository;
    // O0.2.4.d – Repositório de Estatísticas de Ingestão
    private final IngestionBatchStatsRepository ingestionBatchStatsRepository;

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
        NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository,
        IngestionBatchControlRepository ingestionBatchControlRepository,
        IngestionRetryQueueRepository ingestionRetryQueueRepository,
        ValidationRuleRepository validationRuleRepository,
        IngestionBatchStatsRepository ingestionBatchStatsRepository
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
        this.validationRuleRepository = validationRuleRepository;
        this.ingestionBatchStatsRepository = ingestionBatchStatsRepository;
    }

    // -------------------------------------------------------------------------
    // Entrada principal
    // -------------------------------------------------------------------------

    @Transactional
    public ValidationIngestionResponseDto ingerir(List<ValidationIngestionRequestDto> validacoes) {
        if (validacoes == null || validacoes.isEmpty()) {
            return new ValidationIngestionResponseDto(0, 0, 0, 0);
        }

        OffsetDateTime inicioCiclo = OffsetDateTime.now();
        String batchId = UUID.randomUUID().toString();

        // O0.2.1.d: carregar regras ativas da BD
        List<ValidationRule> regrasAtivas = validationRuleRepository.findByAtivoTrue();

        List<ValidationEvent> eventos = new ArrayList<>();
        List<ValidationQuarantine> quarentena = new ArrayList<>();
        List<String> motivosRejeicao = new ArrayList<>();
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
                motivosRejeicao.add("payload_nulo");
                continue;
            }

            // O0.2.1.c: aplica validações lidas da BD
            ValidationResult validacao = validarPayload(dto, regrasAtivas);
            if (!validacao.isValid()) {
                quarentena.add(toQuarentena(dto, validacao));
                motivosRejeicao.add(validacao.getRuleCode());
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
                // O0.2.2.c: normalizar e anonimizar
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
                motivosRejeicao.add("falha_normalizacao");
            }
        }

        validationEventRepository.saveAll(eventos);
        validationQuarantineRepository.saveAll(quarentena);

        // O0.2.3.c: persistir no Data Lake com controlo de lote e retry
        persistirComControloLote(eventos, batchId, inicioCiclo);

        // O0.2.4.c: registar estatísticas do ciclo
        registarEstatisticasCiclo(
            batchId,
            validacoes.size(),
            eventos.size(),
            quarentena.size(),
            duplicadosDescartados,
            motivosRejeicao,
            inicioCiclo
        );

        return new ValidationIngestionResponseDto(
            validacoes.size(),
            eventos.size(),
            quarentena.size(),
            duplicadosDescartados
        );
    }

    // -------------------------------------------------------------------------
    // UC02.1 – O0.2.1.c: Validação com regras lidas da BD (O0.2.1.d)
    // -------------------------------------------------------------------------

    private ValidationResult validarPayload(ValidationIngestionRequestDto dto,
                                            List<ValidationRule> regras) {
        String transactionDateTime = valorOuNull(dto.getTransactionDateTime());
        String originStopId = valorOuNull(dto.getOriginStopId());
        String routeId = valorOuNull(dto.getRouteId());
        String tripId = valorOuNull(dto.getTripId());
        String ticketType = valorOuNull(dto.getTicketTypeCode());
        String result = valorOuNull(dto.getResult());
        String fareForAdult = valorOuNull(dto.getFareForAdult());

        // Campos obrigatórios — lidos das regras OBRIGATORIO na BD
        for (ValidationRule regra : regras) {
            if (!"OBRIGATORIO".equals(regra.getTipoRegra()) || !regra.isAtivo()) continue;
            String valorCampo = getCampoPorNome(dto, regra.getCampo());
            if (valorOuNull(valorCampo) == null) {
                return ValidationResult.invalid(
                    regra.getMotivoRejeicao(), regra.getCampo(), null,
                    "Campo obrigatorio", regra.getRuleCode()
                );
            }
        }

        // Formato ISO 8601
        OffsetDateTime ts;
        try {
            ts = OffsetDateTime.parse(transactionDateTime);
        } catch (Exception e) {
            String motivo = getMotivoRegra(regras, "FORMATO_ISO8601_transactionDateTime");
            return ValidationResult.invalid(motivo, "transactionDateTime", transactionDateTime,
                "Formato ISO-8601", "FORMATO_ISO8601_transactionDateTime");
        }

        // Limite futuro e passado — lidos das regras LIMITE_FUTURO e LIMITE_PASSADO
        long minutesFuturo = getLimiteNumerico(regras, "LIMITE_FUTURO_transactionDateTime", 5L);
        long dayPassado = getLimiteNumerico(regras, "LIMITE_PASSADO_transactionDateTime", 90L);
        OffsetDateTime agora = OffsetDateTime.now();
        if (ts.isAfter(agora.plusMinutes(minutesFuturo))) {
            String motivo = getMotivoRegra(regras, "LIMITE_FUTURO_transactionDateTime");
            return ValidationResult.invalid(motivo, "transactionDateTime", transactionDateTime,
                "Janela: +" + minutesFuturo + " minutos", "LIMITE_FUTURO_transactionDateTime");
        }
        if (ts.isBefore(agora.minusDays(dayPassado))) {
            String motivo = getMotivoRegra(regras, "LIMITE_PASSADO_transactionDateTime");
            return ValidationResult.invalid(motivo, "transactionDateTime", transactionDateTime,
                "Janela: -" + dayPassado + " dias", "LIMITE_PASSADO_transactionDateTime");
        }

        // fareForAdult — formato decimal
        if (fareForAdult != null) {
            try {
                new BigDecimal(fareForAdult);
            } catch (NumberFormatException e) {
                String motivo = getMotivoRegra(regras, "FORMATO_DECIMAL_fareForAdult");
                return ValidationResult.invalid(motivo, "fareForAdult", fareForAdult,
                    "Tipo decimal esperado", "FORMATO_DECIMAL_fareForAdult");
            }
        }

        // ticketTypeCode — lista aprovada lida da BD
        ValidationRule regraLista = getRegra(regras, "LISTA_APROVADA_ticketTypeCode");
        if (regraLista != null) {
            List<String> valoresValidos = Arrays.asList(regraLista.getValorLimite().split(","));
            if (!valoresValidos.contains(ticketType.trim().toUpperCase())) {
                return ValidationResult.invalid(regraLista.getMotivoRejeicao(), "ticketTypeCode",
                    ticketType, "Valores validos: " + regraLista.getValorLimite(),
                    "LISTA_APROVADA_ticketTypeCode");
            }
        }

        // route_id existe no catálogo
        if (!existeLinhaNoCatalogo(routeId)) {
            String motivo = getMotivoRegra(regras, "CATALOGO_route_id");
            return ValidationResult.invalid(motivo, "route_id", routeId,
                "Integridade referencial", "CATALOGO_route_id");
        }

        // trip_id existe e pertence à linha
        Optional<pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Trip> trip =
            tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            String motivo = getMotivoRegra(regras, "CATALOGO_trip_id");
            return ValidationResult.invalid(motivo, "trip_id", tripId,
                "Integridade referencial", "CATALOGO_trip_id");
        }
        if (!routeId.equals(trip.get().getRouteId())) {
            String motivo = getMotivoRegra(regras, "COERENCIA_route_trip");
            return ValidationResult.invalid(motivo, "trip_id", tripId,
                "Coerencia route-trip", "COERENCIA_route_trip");
        }

        // originStopId (opcional) — existe e pertence ao trip
        if (originStopId != null) {
            if (!stopRepository.existsById(originStopId)) {
                String motivo = getMotivoRegra(regras, "CATALOGO_originStopId");
                return ValidationResult.invalid(motivo, "originStopId", originStopId,
                    "Integridade referencial", "CATALOGO_originStopId");
            }
            if (!stopTimesRepository.existsByTripIdAndStopId(tripId, originStopId)) {
                String motivo = getMotivoRegra(regras, "COERENCIA_trip_stop");
                return ValidationResult.invalid(motivo, "originStopId", originStopId,
                    "Coerencia trip-stop", "COERENCIA_trip_stop");
            }
        }

        return ValidationResult.valid();
    }

    // -------------------------------------------------------------------------
    // UC02.2 – O0.2.2.c: Normalização e Anonimização
    // -------------------------------------------------------------------------

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

        FareCollectionSystem fareCollectionSystem =
            resolveFareCollectionSystem(equipmentId, transactionVehicleNum);

        ValidationEvent evento = new ValidationEvent();
        // Pseudonimização irreversível do cardId (UC02.2)
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

    // -------------------------------------------------------------------------
    // UC02.3 – O0.2.3.c: Persistência no Data Lake com controlo de lote e retry
    // -------------------------------------------------------------------------

    private void persistirComControloLote(List<ValidationEvent> eventos,
                                          String batchId,
                                          OffsetDateTime inicio) {
        if (eventos == null || eventos.isEmpty()) return;

        LocalDate particao = LocalDate.now();
        OffsetDateTime agora = OffsetDateTime.now();
        long tempoMs = Duration.between(inicio, agora).toMillis();

        List<NgsiLdDataLakeRecord> records = construirRegistosNgsiLd(eventos, batchId, particao, agora);

        try {
            ngsiLdDataLakeRecordRepository.saveAll(records);

            // Calcular hash do lote após escrita confirmada
            String hashLote = calcularHashLote(eventos);

            ingestionBatchControlRepository.save(new IngestionBatchControl(
                batchId, agora, eventos.size(), records.size(),
                etlMappingVersion, particao, "SUCCESS", tempoMs
            ));

            auditar("BATCH_PERSISTENCE_SUCCESS", "batchId", batchId,
                "Lote persistido. Hash=" + hashLote + " Registos=" + records.size());

        } catch (Exception e) {
            // Falha de persistência → fila de retry com backoff exponencial
            ingestionRetryQueueRepository.save(new IngestionRetryQueue(
                batchId, e.getMessage(), "PENDING",
                0, 10,
                agora.plusSeconds(5), null,
                toJsonEventos(eventos), agora
            ));

            ingestionBatchControlRepository.save(new IngestionBatchControl(
                batchId, agora, eventos.size(), 0,
                etlMappingVersion, particao, "PENDING", tempoMs
            ));

            auditar("BATCH_PERSISTENCE_FAILED", "batchId", batchId,
                "Falha na persistencia. Adicionado a fila de retry. Erro: " + e.getMessage());
        }
    }

    private List<NgsiLdDataLakeRecord> construirRegistosNgsiLd(
        List<ValidationEvent> eventos, String batchId,
        LocalDate particao, OffsetDateTime agora
    ) {
        List<NgsiLdDataLakeRecord> records = new ArrayList<>();
        for (ValidationEvent evento : eventos) {
            try {
                NgsiLdEntityDto entidade = toNgsiLdEntity(evento);
                records.add(new NgsiLdDataLakeRecord(
                    batchId, entidade.getId(), entidade.getType(),
                    toJson(entidade), particao, evento.getTransactionDateTime(),
                    evento.getOriginStop() != null ? evento.getOriginStop().getStopLat() : null,
                    evento.getOriginStop() != null ? evento.getOriginStop().getStopLon() : null,
                    agora
                ));
            } catch (Exception e) {
                auditar("NGSI_LD_CONVERSION_ERROR", evento.getIngestionHash(),
                    e.getMessage(), "Falha na conversao NGSI-LD");
            }
        }
        return records;
    }

    private String calcularHashLote(List<ValidationEvent> eventos) {
        String payload = eventos.stream()
            .map(ValidationEvent::getIngestionHash)
            .collect(Collectors.joining("|"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    // -------------------------------------------------------------------------
    // UC02.4 – O0.2.4.c: Registo de estatísticas e auditoria em O0.2.4.d
    // -------------------------------------------------------------------------

    private void registarEstatisticasCiclo(String batchId, int total, int validos,
                                           int invalidos, int duplicados,
                                           List<String> motivosRejeicao,
                                           OffsetDateTime inicio) {
        long tempoMs = Duration.between(inicio, OffsetDateTime.now()).toMillis();
        boolean alertaQualidade = total > 0 && ((double) invalidos / total) > LIMIAR_QUALIDADE;

        // Resumo dos motivos de rejeição
        String resumoMotivos = motivosRejeicao.stream()
            .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
            .entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));

        // O0.2.4.d: gravar na tabela de estatísticas por lote
        ingestionBatchStatsRepository.save(new IngestionBatchStats(
            batchId,
            OffsetDateTime.now(),
            total, validos, invalidos, duplicados,
            tempoMs,
            invalidos == 0 ? "SUCESSO" : (validos > 0 ? "PARCIAL" : "FALHA"),
            resumoMotivos.isEmpty() ? null : resumoMotivos,
            alertaQualidade
        ));

        // Log de auditoria com resumo
        auditar("INGESTION_CYCLE_STATS", "batchId", batchId,
            String.format("total=%d validos=%d invalidos=%d duplicados=%d tempoMs=%d alerta=%b",
                total, validos, invalidos, duplicados, tempoMs, alertaQualidade));

        // Alerta de qualidade de dados (UC02.4)
        if (alertaQualidade) {
            auditar("DATA_QUALITY_ALERT", "taxaFalha",
                String.format("%.2f%%", (double) invalidos / total * 100),
                String.format("ALERTA: taxa de falha %.1f%% excede limiar de %.0f%%. batchId=%s",
                    (double) invalidos / total * 100, LIMIAR_QUALIDADE * 100, batchId));
        }
    }

    // -------------------------------------------------------------------------
    // Métodos auxiliares
    // -------------------------------------------------------------------------

    private ValidationQuarantine toQuarentena(ValidationIngestionRequestDto dto,
                                              ValidationResult validacao) {
        return new ValidationQuarantine(
            toJson(dto), validacao.getReason(), validacao.getField(),
            validacao.getReceivedValue(), validacao.getRule(), OffsetDateTime.now()
        );
    }

    private FareCollectionSystem resolveFareCollectionSystem(String equipmentId, String vehicleNum) {
        String systemCode = (equipmentId == null ? "SEM_EQUIPAMENTO" : equipmentId)
            + "@" + (vehicleNum == null ? "SEM_VEICULO" : vehicleNum);
        return fareCollectionSystemRepository.findBySystemCode(systemCode)
            .orElseGet(() -> fareCollectionSystemRepository.save(
                new FareCollectionSystem(systemCode, equipmentId, vehicleNum)));
    }

    private String getMotivoRegra(List<ValidationRule> regras, String ruleCode) {
        return regras.stream()
            .filter(r -> ruleCode.equals(r.getRuleCode()))
            .map(ValidationRule::getMotivoRejeicao)
            .findFirst()
            .orElse(ruleCode);
    }

    private ValidationRule getRegra(List<ValidationRule> regras, String ruleCode) {
        return regras.stream()
            .filter(r -> ruleCode.equals(r.getRuleCode()) && r.isAtivo())
            .findFirst()
            .orElse(null);
    }

    private long getLimiteNumerico(List<ValidationRule> regras, String ruleCode, long defaultVal) {
        ValidationRule regra = getRegra(regras, ruleCode);
        if (regra == null || regra.getValorLimite() == null) return defaultVal;
        try {
            return Long.parseLong(regra.getValorLimite());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private String getCampoPorNome(ValidationIngestionRequestDto dto, String campo) {
        return switch (campo) {
            case "transactionDateTime" -> dto.getTransactionDateTime();
            case "route_id" -> dto.getRouteId();
            case "ticketTypeCode" -> dto.getTicketTypeCode();
            case "trip_id" -> dto.getTripId();
            case "result" -> dto.getResult();
            default -> null;
        };
    }

    private String canonicalTicketType(String raw) {
        if (raw == null) return null;
        Map<String, String> mapa = Map.ofEntries(
            Map.entry("PASSE_ESTUDANTE", "PASSE_ESTUDANTE"),
            Map.entry("ESTUDANTE", "PASSE_ESTUDANTE"),
            Map.entry("SINGLE_TICKET", "AVULSO"),
            Map.entry("AVULSO", "AVULSO"),
            Map.entry("MONTHLY_PASS", "MENSAL"),
            Map.entry("MENSAL", "MENSAL"),
            Map.entry("SENIOR_PASS", "PASSE_SENIOR"),
            Map.entry("PASSE_SENIOR", "PASSE_SENIOR")
        );
        return mapa.get(raw.trim().toUpperCase());
    }

    private boolean existeLinhaNoCatalogo(String routeId) {
        try {
            return routeRepository.existsById(Long.parseLong(routeId));
        } catch (NumberFormatException e) {
            return routeRepository.existsByRouteShortName(routeId);
        }
    }

    private String buildIngestionHash(ValidationIngestionRequestDto dto) {
        String payload = String.join("|",
            valorOuVazio(dto.getCardId()), valorOuVazio(dto.getTicketId()),
            valorOuVazio(dto.getMediaType()), valorOuVazio(dto.getTicketTypeCode()),
            valorOuVazio(dto.getTransactionType()), valorOuVazio(dto.getTransactionDateTime()),
            valorOuVazio(dto.getOriginStopId()), valorOuVazio(dto.getRouteId()),
            valorOuVazio(dto.getTripId()), valorOuVazio(dto.getFareForAdult()),
            valorOuVazio(dto.getEquipmentId()), valorOuVazio(dto.getTransactionVehicleNum()),
            valorOuVazio(dto.getResult()), valorOuVazio(dto.getRejectReason())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    private String pseudonimizarIdentificador(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(
                pseudonymSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = hmac.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(valor.hashCode());
        }
    }

    private NgsiLdEntityDto toNgsiLdEntity(ValidationEvent evento) {
        Map<String, Object> props = new HashMap<>();
        props.put("transactionDateTime", ngsiProperty(evento.getTransactionDateTime().toString()));
        props.put("transactionType", ngsiProperty(evento.getTransactionType()));
        props.put("ticketTypeCode", ngsiProperty(evento.getTicketType().getCode()));
        props.put("mediaType", ngsiProperty(evento.getMediaType()));
        props.put("result", ngsiProperty(evento.getResult()));
        props.put("equipmentId", ngsiProperty(evento.getEquipmentId()));
        props.put("transactionVehicleNum", ngsiProperty(evento.getTransactionVehicleNum()));
        props.put("cardId", ngsiProperty(evento.getCardId()));
        if (evento.getFareForAdult() != null)
            props.put("fareForAdult", ngsiProperty(evento.getFareForAdult().doubleValue()));
        if (evento.getRejectReason() != null)
            props.put("rejectReason", ngsiProperty(evento.getRejectReason()));
        if (evento.getOriginStop() != null)
            props.put("originStop", ngsiRelationship("urn:ngsi-ld:Stop:" + evento.getOriginStop().getStopId()));
        if (evento.getRouteId() != null)
            props.put("route", ngsiRelationship("urn:ngsi-ld:Route:" + evento.getRouteId()));
        if (evento.getTripId() != null)
            props.put("trip", ngsiRelationship("urn:ngsi-ld:Trip:" + evento.getTripId()));
        if (evento.getOriginStop() != null
                && evento.getOriginStop().getStopLat() != null
                && evento.getOriginStop().getStopLon() != null)
            props.put("location", ngsiGeoProperty(
                evento.getOriginStop().getStopLat(), evento.getOriginStop().getStopLon()));
        return new NgsiLdEntityDto(
            "urn:ngsi-ld:FareTransaction:" + evento.getIngestionHash(),
            "FareTransaction", etlMappingVersion, props);
    }

    private Map<String, Object> ngsiProperty(Object value) {
        return Map.of("type", "Property", "value", value);
    }

    private Map<String, Object> ngsiRelationship(String objectUrn) {
        return Map.of("type", "Relationship", "object", objectUrn);
    }

    private Map<String, Object> ngsiGeoProperty(Double lat, Double lon) {
        return Map.of("type", "GeoProperty", "value",
            Map.of("type", "Point", "coordinates", List.of(lon, lat)));
    }

    private String valorOuNull(String valor) {
        if (valor == null) return null;
        String t = valor.trim();
        return t.isEmpty() ? null : t;
    }

    private String valorOuVazio(String valor) {
        String t = valorOuNull(valor);
        return t == null ? "" : t;
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }

    private String toJsonEventos(List<ValidationEvent> eventos) {
        try { return objectMapper.writeValueAsString(eventos); }
        catch (Exception e) { return "[]"; }
    }

    private void auditar(String eventType, String fieldName, String originalValue, String detail) {
        ingestionAuditLogRepository.save(
            new IngestionAuditLog(eventType, fieldName, originalValue, detail, OffsetDateTime.now()));
    }

    // -------------------------------------------------------------------------
    // Classe interna de resultado de validação
    // -------------------------------------------------------------------------

    private static class ValidationResult {
        private final boolean valid;
        private final String reason;
        private final String field;
        private final String receivedValue;
        private final String rule;
        private final String ruleCode;

        private ValidationResult(boolean valid, String reason, String field,
                                 String receivedValue, String rule, String ruleCode) {
            this.valid = valid; this.reason = reason; this.field = field;
            this.receivedValue = receivedValue; this.rule = rule; this.ruleCode = ruleCode;
        }

        static ValidationResult valid() {
            return new ValidationResult(true, null, null, null, null, null);
        }

        static ValidationResult invalid(String reason, String field, String receivedValue,
                                        String rule, String ruleCode) {
            return new ValidationResult(false, reason, field, receivedValue, rule, ruleCode);
        }

        boolean isValid() { return valid; }
        String getReason() { return reason; }
        String getField() { return field; }
        String getReceivedValue() { return receivedValue; }
        String getRule() { return rule; }
        String getRuleCode() { return ruleCode; }
    }
}

