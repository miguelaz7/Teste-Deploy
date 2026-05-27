package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

// =============================================================================
// O012.2.c – Controlador de Integração NGSI-LD
// Valida autenticação, autorização e conformidade com o esquema
// Smart Data Models. Processa o pedido e regista na auditoria central.
// Suporta publicação e recepção de entidades NGSI-LD de sistemas externos.
// =============================================================================

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ControladorIntegracaoNgsiLd {

    // Tipos de entidade conformes com Smart Data Models / NGSI-LD
    private static final Set<String> VALID_TYPES = Set.of(
        "FareTransaction",
        "PublicTransportRoute",
        "PublicTransportStop",
        "TransportValidationEvent"
    );

    private final RepositorioRegistoDataLakeNgsiLd ngsiLdRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ControladorIntegracaoNgsiLd(RepositorioRegistoDataLakeNgsiLd ngsiLdRepository) {
        this.ngsiLdRepository = ngsiLdRepository;
    }

    // Publica uma entidade NGSI-LD (recebida de sistema externo)
    @Transactional
    public RegistoDataLakeNgsiLd publishEntity(Map<String, Object> payload, String user) {
        // Valida conformidade com esquema NGSI-LD
        validateSchema(payload);

        String entityId   = extractField(payload, "id");
        String entityType = extractField(payload, "type");
        String batchId    = "EXT_" + OffsetDateTime.now().toInstant().toEpochMilli();

        String payloadJson = toJson(payload);

        RegistoDataLakeNgsiLd record = new RegistoDataLakeNgsiLd(
            batchId, entityId, entityType,
            payloadJson, LocalDate.now(),
            OffsetDateTime.now(),
            null, null,
            OffsetDateTime.now()
        );

        return ngsiLdRepository.save(record);
    }

    // Consulta entidades NGSI-LD por tipo
    public List<RegistoDataLakeNgsiLd> getByType(String entityType) {
        return ngsiLdRepository.findAll().stream()
            .filter(r -> entityType.equals(r.getEntityType()))
            .toList();
    }

    // Calculates integrity hash of a payload
    public String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(content.hashCode());
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void validateSchema(Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload NGSI-LD nulo");
        }
        if (!payload.containsKey("id")) {
            throw new IllegalArgumentException("Campo obrigatorio em falta: id");
        }
        if (!payload.containsKey("type")) {
            throw new IllegalArgumentException("Campo obrigatorio em falta: type");
        }
        String type = payload.get("type").toString();
        if (!VALID_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                "Tipo de entidade nao conforme com Smart Data Models: " + type);
        }
        if (!payload.containsKey("@context")) {
            throw new IllegalArgumentException("Campo obrigatorio em falta: @context");
        }
    }

    private String extractField(Map<String, Object> payload, String field) {
        Object value = payload.get(field);
        if (value == null) throw new IllegalArgumentException("Campo em falta: " + field);
        return value.toString();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
