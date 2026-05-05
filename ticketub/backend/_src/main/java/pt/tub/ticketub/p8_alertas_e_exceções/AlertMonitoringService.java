package pt.tub.ticketub.p8_alertas_e_excecoes;

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class AlertMonitoringService {

    private static final Set<String> VALID_RESULTS = Set.of(
        "VALID",
        "OK",
        "SUCCESS",
        "ACEITE",
        "ACEITO",
        "APROVADO",
        "1"
    );

    private static final DateTimeFormatter BUCKET_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH").withZone(ZoneOffset.UTC);

    private final ValidationEventAlertSource validationEventSource;
    private final AlertIncidentStore alertIncidentStore;
    private final AlertConfigurationStore alertConfigurationStore;
    private final AlertActionLogStore alertActionLogStore;

    public AlertMonitoringService(
        ValidationEventRepository validationEventRepository,
        AlertIncidentStore alertIncidentStore,
        AlertConfigurationStore alertConfigurationStore,
        AlertActionLogStore alertActionLogStore
    ) {
        this.validationEventSource = validationEventRepository;
        this.alertIncidentStore = alertIncidentStore;
        this.alertConfigurationStore = alertConfigurationStore;
        this.alertActionLogStore = alertActionLogStore;
    }

    @Scheduled(fixedDelayString = "${ticketub.alerts.scan-delay-ms:60000}")
    public void runScheduledDetection() {
        runDetection();
    }

    public AlertDetectionResponseDto runDetection() {
        AlertConfigurationEntity configuration = ensureConfiguration();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime windowStart = now.minusMinutes(configuration.getDetectionWindowMinutes());
        List<ValidationEvent> events = validationEventSource.findByTransactionDateTimeAfter(windowStart);

        List<AlertIncidentResponseDto> generatedAlerts = new ArrayList<>();
        int createdAlerts = 0;
        int updatedAlerts = 0;
        int escalatedAlerts = 0;

        for (ValidationEvent event : events) {
            List<AlertDraft> drafts = buildDraftsForEvent(event, now, configuration);
            for (AlertDraft draft : drafts) {
                UpsertResult result = upsertAlert(draft, now);
                generatedAlerts.add(AlertIncidentResponseDto.fromEntity(result.alertIncident()));
                if (result.created()) {
                    createdAlerts++;
                } else {
                    updatedAlerts++;
                }
                if (result.escalated()) {
                    escalatedAlerts++;
                }
            }
        }

        for (AlertDraft draft : buildAggregateDrafts(events, now, configuration)) {
            UpsertResult result = upsertAlert(draft, now);
            generatedAlerts.add(AlertIncidentResponseDto.fromEntity(result.alertIncident()));
            if (result.created()) {
                createdAlerts++;
            } else {
                updatedAlerts++;
            }
            if (result.escalated()) {
                escalatedAlerts++;
            }
        }

        return new AlertDetectionResponseDto(now, events.size(), createdAlerts, updatedAlerts, escalatedAlerts, generatedAlerts);
    }

    @Transactional(readOnly = true)
    public List<AlertIncidentResponseDto> listAlerts(Optional<AlertStatus> status, Optional<AlertSeverity> severity, Optional<AlertCategory> category) {
        return alertIncidentStore.findAll().stream()
            .filter(alert -> status.map(value -> alert.getStatus() == value).orElse(true))
            .filter(alert -> severity.map(value -> alert.getSeverity() == value).orElse(true))
            .filter(alert -> category.map(value -> alert.getCategory() == value).orElse(true))
            .sorted(Comparator.comparing(AlertIncidentEntity::getDetectedAt).reversed())
            .map(AlertIncidentResponseDto::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public AlertIncidentResponseDto getAlert(Long id) {
        return AlertIncidentResponseDto.fromEntity(findAlertOrFail(id));
    }

    public AlertIncidentResponseDto updateAlertStatus(Long id, AlertStatusUpdateRequestDto request) {
        AlertIncidentEntity alert = findAlertOrFail(id);
        applyStatusUpdate(alert, request.status(), request.actor(), request.notes());
        AlertIncidentEntity saved = alertIncidentStore.save(alert);
        registerAction(saved, AlertActionType.ACKNOWLEDGE, request.actor(), request.notes());
        return AlertIncidentResponseDto.fromEntity(saved);
    }

    public AlertIncidentResponseDto registerAction(Long id, AlertActionRequestDto request) {
        AlertIncidentEntity alert = findAlertOrFail(id);
        applyAction(alert, request.actionType(), request.actor(), request.notes());
        AlertIncidentEntity saved = alertIncidentStore.save(alert);
        registerAction(saved, request.actionType(), request.actor(), request.notes());
        return AlertIncidentResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AlertConfigurationResponseDto getConfiguration() {
        return AlertConfigurationResponseDto.fromEntity(ensureConfiguration());
    }

    public AlertConfigurationResponseDto updateConfiguration(AlertConfigurationRequestDto request) {
        AlertConfigurationEntity configuration = ensureConfiguration();
        configuration.setDetectionWindowMinutes(request.detectionWindowMinutes());
        configuration.setRepeatCardThreshold(request.repeatCardThreshold());
        configuration.setVolumeSpikeThreshold(request.volumeSpikeThreshold());
        configuration.setCriticalInvalidRatio(request.criticalInvalidRatio());
        configuration.setMinEventsForRatio(request.minEventsForRatio());
        return AlertConfigurationResponseDto.fromEntity(alertConfigurationStore.save(configuration));
    }

    @Transactional(readOnly = true)
    public AlertSummaryResponseDto getSummary() {
        List<AlertIncidentEntity> alerts = alertIncidentStore.findAll();
        return new AlertSummaryResponseDto(
            alerts.size(),
            countByStatus(alerts, AlertStatus.OPEN),
            countByStatus(alerts, AlertStatus.PENDING),
            countByStatus(alerts, AlertStatus.IN_PROGRESS),
            countByStatus(alerts, AlertStatus.FALSE_POSITIVE),
            countByStatus(alerts, AlertStatus.RESOLVED),
            countByStatus(alerts, AlertStatus.ESCALATED),
            countBySeverity(alerts, AlertSeverity.CRITICAL)
        );
    }

    private List<AlertDraft> buildDraftsForEvent(ValidationEvent event, OffsetDateTime now, AlertConfigurationEntity configuration) {
        List<AlertDraft> drafts = new ArrayList<>();

        if (isMissingCriticalData(event)) {
            drafts.add(new AlertDraft(
                fingerprint("MISSING_DATA", identifierFor(event)),
                AlertCategory.MISSING_DATA,
                AlertSeverity.LOW,
                AlertStatus.PENDING,
                buildTitle("Dados em falta"),
                buildMissingDataDescription(event),
                event.getRouteId(),
                stopId(event),
                event.getTripId(),
                event.getCardId(),
                event.getTicketId(),
                event.getId(),
                false,
                now
            ));
        }

        if (isInvalidValidation(event)) {
            drafts.add(new AlertDraft(
                fingerprint("INVALID", identifierFor(event)),
                AlertCategory.INVALID_VALIDATION,
                determineInvalidSeverity(event),
                determineInvalidSeverity(event) == AlertSeverity.CRITICAL ? AlertStatus.ESCALATED : AlertStatus.OPEN,
                buildTitle("Validação anómala"),
                buildInvalidDescription(event),
                event.getRouteId(),
                stopId(event),
                event.getTripId(),
                event.getCardId(),
                event.getTicketId(),
                event.getId(),
                determineInvalidSeverity(event) == AlertSeverity.CRITICAL,
                now
            ));
        }

        return drafts;
    }

    private List<AlertDraft> buildAggregateDrafts(List<ValidationEvent> events, OffsetDateTime now, AlertConfigurationEntity configuration) {
        List<AlertDraft> drafts = new ArrayList<>();

        Map<String, List<ValidationEvent>> eventsByRoute = events.stream()
            .collect(Collectors.groupingBy(event -> routeKey(event), LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<ValidationEvent>> entry : eventsByRoute.entrySet()) {
            List<ValidationEvent> routeEvents = entry.getValue();
            long invalidCount = routeEvents.stream().filter(this::isInvalidValidation).count();
            long missingDataCount = routeEvents.stream().filter(this::isMissingCriticalData).count();
            long suspiciousCount = invalidCount + missingDataCount;
            double suspiciousRatio = routeEvents.isEmpty() ? 0.0 : (double) suspiciousCount / routeEvents.size();

            if (routeEvents.size() >= configuration.getVolumeSpikeThreshold()
                && routeEvents.size() >= configuration.getMinEventsForRatio()
                && suspiciousRatio >= configuration.getCriticalInvalidRatio()) {

                drafts.add(new AlertDraft(
                    fingerprint("VOLUME_SPIKE", entry.getKey(), bucket(now)),
                    AlertCategory.VOLUME_SPIKE,
                    AlertSeverity.CRITICAL,
                    AlertStatus.ESCALATED,
                    buildTitle("Pico de volume detetado"),
                    "Foram observadas " + routeEvents.size() + " validações na linha " + entry.getKey()
                        + " com " + suspiciousCount + " ocorrências suspeitas.",
                    routeEvents.get(0).getRouteId(),
                    stopId(routeEvents.get(0)),
                    routeEvents.get(0).getTripId(),
                    routeEvents.get(0).getCardId(),
                    routeEvents.get(0).getTicketId(),
                    routeEvents.get(0).getId(),
                    true,
                    now
                ));
            }
        }

        Map<String, List<ValidationEvent>> eventsByCard = events.stream()
            .filter(event -> hasText(event.getCardId()))
            .collect(Collectors.groupingBy(ValidationEvent::getCardId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<ValidationEvent>> entry : eventsByCard.entrySet()) {
            List<ValidationEvent> cardEvents = entry.getValue();
            Set<String> distinctStops = cardEvents.stream().map(this::stopId).collect(Collectors.toCollection(LinkedHashSet::new));

            if (cardEvents.size() >= configuration.getRepeatCardThreshold() && distinctStops.size() >= 2) {
                drafts.add(new AlertDraft(
                    fingerprint("CARD_REUSE", entry.getKey(), bucket(now)),
                    AlertCategory.REPEATED_CARD_USE,
                    AlertSeverity.HIGH,
                    AlertStatus.OPEN,
                    buildTitle("Uso repetido de cartão"),
                    "O cartão " + entry.getKey() + " foi observado " + cardEvents.size()
                        + " vezes em " + distinctStops.size() + " paragens distintas.",
                    cardEvents.get(0).getRouteId(),
                    stopId(cardEvents.get(0)),
                    cardEvents.get(0).getTripId(),
                    cardEvents.get(0).getCardId(),
                    cardEvents.get(0).getTicketId(),
                    cardEvents.get(0).getId(),
                    false,
                    now
                ));
            }
        }

        return drafts;
    }

    private UpsertResult upsertAlert(AlertDraft draft, OffsetDateTime now) {
        AlertIncidentEntity target = findOpenAlertByFingerprint(draft.fingerprint()).orElseGet(AlertIncidentEntity::new);
        boolean created = target.getId() == null;
        boolean escalated = false;

        if (created) {
            populateAlert(target, draft, now);
            target.setObservedCount(1);
            target.setDetectedAt(now);
            target.setUpdatedAt(now);
            target.setLastOccurrenceAt(now);
            AlertIncidentEntity saved = alertIncidentStore.save(target);
            registerAction(saved, AlertActionType.ACKNOWLEDGE, "SYSTEM", "Alerta criado automaticamente");
            return new UpsertResult(saved, true, saved.isEscalatedToControlCenter());
        }

        AlertSeverity previousSeverity = target.getSeverity();
        AlertStatus previousStatus = target.getStatus();
        int previousObservedCount = target.getObservedCount();

        mergeAlert(target, draft, now);
        target.setObservedCount(previousObservedCount + 1);
        target.setUpdatedAt(now);
        target.setLastOccurrenceAt(now);

        escalated = target.isEscalatedToControlCenter() && !previousStatus.equals(AlertStatus.ESCALATED);
        AlertIncidentEntity saved = alertIncidentStore.save(target);
        boolean escalatedNow = saved.isEscalatedToControlCenter() || saved.getStatus() == AlertStatus.ESCALATED;
        if (!previousSeverity.equals(saved.getSeverity()) || !previousStatus.equals(saved.getStatus())) {
            registerAction(saved, AlertActionType.INVESTIGATE, "SYSTEM", "Alerta atualizado automaticamente");
        }
        return new UpsertResult(saved, false, escalatedNow || escalated);
    }

    private void populateAlert(AlertIncidentEntity alert, AlertDraft draft, OffsetDateTime now) {
        alert.setAlertFingerprint(draft.fingerprint());
        alert.setCategory(draft.category());
        alert.setSeverity(draft.severity());
        alert.setStatus(draft.status());
        alert.setTitle(draft.title());
        alert.setDescription(draft.description());
        alert.setRouteId(draft.routeId());
        alert.setStopId(draft.stopId());
        alert.setTripId(draft.tripId());
        alert.setCardId(draft.cardId());
        alert.setTicketId(draft.ticketId());
        alert.setSourceValidationEventId(draft.sourceValidationEventId());
        alert.setEscalatedToControlCenter(draft.escalatedToControlCenter());
        alert.setDetectedAt(now);
        alert.setUpdatedAt(now);
        alert.setLastOccurrenceAt(draft.lastOccurrenceAt());
    }

    private void mergeAlert(AlertIncidentEntity alert, AlertDraft draft, OffsetDateTime now) {
        alert.setCategory(draft.category());
        alert.setSeverity(higherSeverity(alert.getSeverity(), draft.severity()));
        alert.setStatus(mergeStatus(alert.getStatus(), draft.status(), draft.severity()));
        alert.setTitle(draft.title());
        alert.setDescription(draft.description());
        alert.setRouteId(firstNonBlank(alert.getRouteId(), draft.routeId()));
        alert.setStopId(firstNonBlank(alert.getStopId(), draft.stopId()));
        alert.setTripId(firstNonBlank(alert.getTripId(), draft.tripId()));
        alert.setCardId(firstNonBlank(alert.getCardId(), draft.cardId()));
        alert.setTicketId(firstNonBlank(alert.getTicketId(), draft.ticketId()));
        alert.setSourceValidationEventId(firstNonNull(alert.getSourceValidationEventId(), draft.sourceValidationEventId()));
        alert.setEscalatedToControlCenter(alert.isEscalatedToControlCenter() || draft.escalatedToControlCenter());
        if (draft.severity() == AlertSeverity.CRITICAL) {
            alert.setEscalatedToControlCenter(true);
        }
        if (alert.getStatus() == AlertStatus.RESOLVED || alert.getStatus() == AlertStatus.FALSE_POSITIVE) {
            alert.setStatus(draft.status());
        }
        alert.setLastOccurrenceAt(now);
    }

    private void applyStatusUpdate(AlertIncidentEntity alert, AlertStatus status, String actor, String notes) {
        alert.setStatus(status);
        alert.setLastActionBy(actor);
        alert.setLastActionNote(notes);
        alert.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (status == AlertStatus.RESOLVED) {
            alert.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        if (status == AlertStatus.FALSE_POSITIVE) {
            alert.setFalsePositiveCount(alert.getFalsePositiveCount() + 1);
        }
        if (status == AlertStatus.ESCALATED) {
            alert.setEscalatedToControlCenter(true);
        }
    }

    private void applyAction(AlertIncidentEntity alert, AlertActionType actionType, String actor, String notes) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        alert.setLastActionBy(actor);
        alert.setLastActionNote(notes);
        alert.setUpdatedAt(now);

        switch (actionType) {
            case ACKNOWLEDGE, INVESTIGATE -> alert.setStatus(AlertStatus.IN_PROGRESS);
            case RESOLVE -> {
                alert.setStatus(AlertStatus.RESOLVED);
                alert.setResolvedAt(now);
            }
            case MARK_FALSE_POSITIVE -> {
                alert.setStatus(AlertStatus.FALSE_POSITIVE);
                alert.setFalsePositiveCount(alert.getFalsePositiveCount() + 1);
            }
            case ESCALATE -> {
                alert.setStatus(AlertStatus.ESCALATED);
                alert.setEscalatedToControlCenter(true);
            }
        }
    }

    private void registerAction(AlertIncidentEntity alert, AlertActionType actionType, String actor, String notes) {
        AlertActionLogEntity log = new AlertActionLogEntity();
        log.setAlertIncident(alert);
        log.setActionType(actionType);
        log.setActor(actor);
        log.setNotes(notes);
        log.setResultingStatus(alert.getStatus());
        log.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        alertActionLogStore.save(log);
    }

    private AlertConfigurationEntity ensureConfiguration() {
        return alertConfigurationStore.findTopByOrderByIdAsc().orElseGet(() -> alertConfigurationStore.save(new AlertConfigurationEntity()));
    }

    private AlertIncidentEntity findAlertOrFail(Long id) {
        return alertIncidentStore.findById(id)
            .orElseThrow(() -> new AlertNotFoundException("Alerta não encontrado: " + id));
    }

    private Optional<AlertIncidentEntity> findOpenAlertByFingerprint(String fingerprint) {
        return alertIncidentStore.findAllByAlertFingerprintOrderByDetectedAtDesc(fingerprint).stream()
            .filter(alert -> alert.getStatus() != AlertStatus.RESOLVED && alert.getStatus() != AlertStatus.FALSE_POSITIVE)
            .findFirst();
    }

    private String buildTitle(String prefix) {
        return prefix + " - UC08";
    }

    private boolean isInvalidValidation(ValidationEvent event) {
        if (hasText(event.getRejectReason())) {
            return true;
        }
        String result = event.getResult();
        if (!hasText(result)) {
            return true;
        }
        return !VALID_RESULTS.contains(result.trim().toUpperCase(Locale.ROOT));
    }

    private boolean isMissingCriticalData(ValidationEvent event) {
        return !hasText(event.getRouteId()) || event.getOriginStop() == null || !hasText(event.getTripId());
    }

    private AlertSeverity determineInvalidSeverity(ValidationEvent event) {
        String reason = Optional.ofNullable(event.getRejectReason()).orElse("").toUpperCase(Locale.ROOT);
        if (reason.contains("FRAUD") || reason.contains("TAMPER") || reason.contains("SUSPECT") || reason.contains("EVAS")) {
            return AlertSeverity.CRITICAL;
        }
        if (reason.contains("DUPLICATE") || reason.contains("TIMEOUT") || reason.contains("INVALID")) {
            return AlertSeverity.HIGH;
        }
        return AlertSeverity.MEDIUM;
    }

    private AlertStatus mergeStatus(AlertStatus current, AlertStatus incoming, AlertSeverity incomingSeverity) {
        if (current == AlertStatus.RESOLVED || current == AlertStatus.FALSE_POSITIVE) {
            return incoming;
        }
        if (incoming == AlertStatus.ESCALATED || incomingSeverity == AlertSeverity.CRITICAL) {
            return AlertStatus.ESCALATED;
        }
        if (current == AlertStatus.PENDING || incoming == AlertStatus.PENDING) {
            return AlertStatus.PENDING;
        }
        if (current == AlertStatus.IN_PROGRESS || incoming == AlertStatus.IN_PROGRESS) {
            return AlertStatus.IN_PROGRESS;
        }
        return AlertStatus.OPEN;
    }

    private AlertSeverity higherSeverity(AlertSeverity current, AlertSeverity incoming) {
        return current.ordinal() >= incoming.ordinal() ? current : incoming;
    }

    private String buildInvalidDescription(ValidationEvent event) {
        return "Resultado inválido para a validação " + identifierFor(event)
            + ". Motivo: " + Optional.ofNullable(event.getRejectReason()).orElse("não especificado") + ".";
    }

    private String buildMissingDataDescription(ValidationEvent event) {
        List<String> missing = new ArrayList<>();
        if (!hasText(event.getRouteId())) {
            missing.add("routeId");
        }
        if (event.getOriginStop() == null) {
            missing.add("originStop");
        }
        if (!hasText(event.getTripId())) {
            missing.add("tripId");
        }
        return "Classificação pendente por falta de dados: " + String.join(", ", missing) + ".";
    }

    private String routeKey(ValidationEvent event) {
        if (hasText(event.getRouteId())) {
            return event.getRouteId();
        }
        if (event.getOriginStop() != null && hasText(event.getOriginStop().getStopId())) {
            return event.getOriginStop().getStopId();
        }
        if (hasText(event.getTripId())) {
            return event.getTripId();
        }
        return "SEM_LINHA";
    }

    private String stopId(ValidationEvent event) {
        if (event.getOriginStop() != null && hasText(event.getOriginStop().getStopId())) {
            return event.getOriginStop().getStopId();
        }
        return null;
    }

    private String identifierFor(ValidationEvent event) {
        if (event.getId() != null) {
            return "id-" + event.getId();
        }
        if (hasText(event.getIngestionHash())) {
            return "hash-" + event.getIngestionHash();
        }
        return firstNonBlank(event.getTicketId(), event.getCardId(), event.getTransactionDateTime() == null ? null : event.getTransactionDateTime().toString());
    }

    private String fingerprint(String prefix, String identifier) {
        return prefix + ":" + identifier;
    }

    private String fingerprint(String prefix, String identifier, String bucket) {
        return prefix + ":" + identifier + ":" + bucket;
    }

    private String bucket(OffsetDateTime dateTime) {
        return BUCKET_FORMAT.format(dateTime);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private <T> T firstNonNull(T current, T incoming) {
        return current != null ? current : incoming;
    }

    private long countByStatus(List<AlertIncidentEntity> alerts, AlertStatus status) {
        return alerts.stream().filter(alert -> alert.getStatus() == status).count();
    }

    private long countBySeverity(List<AlertIncidentEntity> alerts, AlertSeverity severity) {
        return alerts.stream().filter(alert -> alert.getSeverity() == severity).count();
    }

    private record AlertDraft(
        String fingerprint,
        AlertCategory category,
        AlertSeverity severity,
        AlertStatus status,
        String title,
        String description,
        String routeId,
        String stopId,
        String tripId,
        String cardId,
        String ticketId,
        Long sourceValidationEventId,
        boolean escalatedToControlCenter,
        OffsetDateTime lastOccurrenceAt
    ) {
    }

    private record UpsertResult(
        AlertIncidentEntity alertIncident,
        boolean created,
        boolean escalated
    ) {
    }
}