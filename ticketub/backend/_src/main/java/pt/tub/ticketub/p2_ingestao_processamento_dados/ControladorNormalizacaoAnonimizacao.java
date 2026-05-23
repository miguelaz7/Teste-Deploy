package pt.tub.ticketub.p2_ingestao_processamento_dados;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.FareCollectionSystem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.FareCollectionSystemRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Stop;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TicketType;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TicketTypeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Map;

// =============================================================================
// O0.2.2.c – Controlador de Normalização e Anonimização
// Aplica mapeamento para formato alvo (ValidationEvent) e pseudonimização
// irreversível do identificador do cartão antes do armazenamento final (O0.2.2.d).
// =============================================================================

@Service
class ControladorNormalizacaoAnonimizacao {

    private static final Map<String, String> TICKET_TYPES_CANONICOS = Map.ofEntries(
        Map.entry("PASSE_ESTUDANTE", "PASSE_ESTUDANTE"),
        Map.entry("ESTUDANTE",       "PASSE_ESTUDANTE"),
        Map.entry("SINGLE_TICKET",   "AVULSO"),
        Map.entry("AVULSO",          "AVULSO"),
        Map.entry("MONTHLY_PASS",    "MENSAL"),
        Map.entry("MENSAL",          "MENSAL"),
        Map.entry("SENIOR_PASS",     "PASSE_SENIOR"),
        Map.entry("PASSE_SENIOR",    "PASSE_SENIOR")
    );

    @Value("${app.ingestion.pseudonym.secret:dev-secret-change-me}")
    private String pseudonymSecret;

    private final TicketTypeRepository ticketTypeRepository;
    private final StopRepository stopRepository;
    private final FareCollectionSystemRepository fareCollectionSystemRepository;

    ControladorNormalizacaoAnonimizacao(TicketTypeRepository ticketTypeRepository,
                                        StopRepository stopRepository,
                                        FareCollectionSystemRepository fareCollectionSystemRepository) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.stopRepository = stopRepository;
        this.fareCollectionSystemRepository = fareCollectionSystemRepository;
    }

    // Normaliza o DTO para ValidationEvent e pseudonimiza o cardId
    ValidationEvent normalizar(ValidationIngestionRequestDto dto, String ingestionHash) {
        String mediaType           = valorOuNull(dto.getMediaType());
        String ticketTypeCode      = canonicalTicketType(valorOuNull(dto.getTicketTypeCode()));
        String transactionType     = valorOuNull(dto.getTransactionType());
        String transactionDateTime = valorOuNull(dto.getTransactionDateTime());
        String originStopId        = valorOuNull(dto.getOriginStopId());
        String routeId             = valorOuNull(dto.getRouteId());
        String tripId              = valorOuNull(dto.getTripId());
        String fareForAdult        = valorOuNull(dto.getFareForAdult());
        String equipmentId         = valorOuNull(dto.getEquipmentId());
        String vehicleNum          = valorOuNull(dto.getTransactionVehicleNum());
        String result              = valorOuNull(dto.getResult());
        String rejectReason        = valorOuNull(dto.getRejectReason());

        TicketType ticketType = ticketTypeRepository.findByCode(ticketTypeCode)
            .orElseGet(() -> ticketTypeRepository.save(new TicketType(ticketTypeCode, ticketTypeCode)));

        Stop stop = (originStopId != null)
            ? stopRepository.findById(originStopId).orElse(null)
            : null;

        FareCollectionSystem fcs = resolveFareCollectionSystem(equipmentId, vehicleNum);

        ValidationEvent evento = new ValidationEvent();
        evento.setCardId(pseudonimizar(valorOuNull(dto.getCardId())));
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
        evento.setFareCollectionSystem(fcs);
        evento.setFareForAdult(fareForAdult == null ? null : new BigDecimal(fareForAdult));
        evento.setEquipmentId(equipmentId);
        evento.setTransactionVehicleNum(vehicleNum);
        evento.setResult(result);
        evento.setRejectReason(rejectReason);

        return evento;
    }

    // Calcula o hash de identificação única da picagem (deduplicação)
    String calcularIngestionHash(ValidationIngestionRequestDto dto) {
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

    // -------------------------------------------------------------------------
    // Auxiliares privados
    // -------------------------------------------------------------------------

    private String pseudonimizar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(pseudonymSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = hmac.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(valor.hashCode());
        }
    }

    private FareCollectionSystem resolveFareCollectionSystem(String equipmentId, String vehicleNum) {
        String code = (equipmentId == null ? "SEM_EQUIPAMENTO" : equipmentId)
                    + "@" + (vehicleNum == null ? "SEM_VEICULO" : vehicleNum);
        return fareCollectionSystemRepository.findBySystemCode(code)
            .orElseGet(() -> fareCollectionSystemRepository.save(
                new FareCollectionSystem(code, equipmentId, vehicleNum)));
    }

    private String canonicalTicketType(String raw) {
        if (raw == null) return null;
        return TICKET_TYPES_CANONICOS.get(raw.trim().toUpperCase());
    }

    private String valorOuNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private String valorOuVazio(String v) {
        String t = valorOuNull(v);
        return t == null ? "" : t;
    }
}