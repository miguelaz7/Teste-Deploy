package pt.tub.ticketub.p2_ingestao_processamento_dados;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.SistemaBilhetica;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioSistemaBilhetica;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Paragem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioParagem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TipoBilhete;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioTipoBilhete;
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
// Aplica mapeamento para formato alvo (EventoValidacao) e pseudonimização
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

    private final RepositorioTipoBilhete repositorioTipoBilhete;
    private final RepositorioParagem repositorioParagem;
    private final RepositorioSistemaBilhetica repositorioSistemaBilhetica;

    ControladorNormalizacaoAnonimizacao(RepositorioTipoBilhete repositorioTipoBilhete,
                                        RepositorioParagem repositorioParagem,
                                        RepositorioSistemaBilhetica repositorioSistemaBilhetica) {
        this.repositorioTipoBilhete = repositorioTipoBilhete;
        this.repositorioParagem = repositorioParagem;
        this.repositorioSistemaBilhetica = repositorioSistemaBilhetica;
    }

    // Normaliza o DTO para EventoValidacao e pseudonimiza o cardId
    EventoValidacao normalize(DtoPedidoIngestaoValidacao dto, String ingestionHash) {
        String mediaType           = valueOrNull(dto.getMediaType());
        String ticketTypeCode      = canonicalTicketType(valueOrNull(dto.getTicketTypeCode()));
        String transactionType     = valueOrNull(dto.getTransactionType());
        String transactionDateTime = valueOrNull(dto.getTransactionDateTime());
        String originStopId        = valueOrNull(dto.getOriginStopId());
        String routeId             = valueOrNull(dto.getRouteId());
        String tripId              = valueOrNull(dto.getTripId());
        String fareForAdult        = valueOrNull(dto.getFareForAdult());
        String equipmentId         = valueOrNull(dto.getEquipmentId());
        String vehicleNum          = valueOrNull(dto.getTransactionVehicleNum());
        String result              = valueOrNull(dto.getResult());
        String rejectReason        = valueOrNull(dto.getRejectReason());

        TipoBilhete ticketType = repositorioTipoBilhete.findByCode(ticketTypeCode)
            .orElseGet(() -> repositorioTipoBilhete.save(new TipoBilhete(ticketTypeCode, ticketTypeCode)));

        Paragem stop = (originStopId != null)
            ? repositorioParagem.findById(originStopId).orElse(null)
            : null;

        SistemaBilhetica fcs = resolveFareCollectionSystem(equipmentId, vehicleNum);

        EventoValidacao evento = new EventoValidacao();
        evento.setCardId(pseudonymize(valueOrNull(dto.getCardId())));
        evento.setTicketId(valueOrNull(dto.getTicketId()));
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

    // Calculates the unique hash of a validation (deduplication)
    String calculateIngestionHash(DtoPedidoIngestaoValidacao dto) {
        String payload = String.join("|",
            valueOrEmpty(dto.getCardId()), valueOrEmpty(dto.getTicketId()),
            valueOrEmpty(dto.getMediaType()), valueOrEmpty(dto.getTicketTypeCode()),
            valueOrEmpty(dto.getTransactionType()), valueOrEmpty(dto.getTransactionDateTime()),
            valueOrEmpty(dto.getOriginStopId()), valueOrEmpty(dto.getRouteId()),
            valueOrEmpty(dto.getTripId()), valueOrEmpty(dto.getFareForAdult()),
            valueOrEmpty(dto.getEquipmentId()), valueOrEmpty(dto.getTransactionVehicleNum()),
            valueOrEmpty(dto.getResult()), valueOrEmpty(dto.getRejectReason())
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

    private String pseudonymize(String valor) {
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

    private SistemaBilhetica resolveFareCollectionSystem(String equipmentId, String vehicleNum) {
        String code = (equipmentId == null ? "SEM_EQUIPAMENTO" : equipmentId)
                    + "@" + (vehicleNum == null ? "SEM_VEICULO" : vehicleNum);
        return repositorioSistemaBilhetica.findBySystemCode(code)
            .orElseGet(() -> repositorioSistemaBilhetica.save(
                new SistemaBilhetica(code, equipmentId, vehicleNum)));
    }

    private String canonicalTicketType(String raw) {
        if (raw == null) return null;
        return TICKET_TYPES_CANONICOS.get(raw.trim().toUpperCase());
    }

    private String valueOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private String valueOrEmpty(String v) {
        String t = valueOrNull(v);
        return t == null ? "" : t;
    }
}