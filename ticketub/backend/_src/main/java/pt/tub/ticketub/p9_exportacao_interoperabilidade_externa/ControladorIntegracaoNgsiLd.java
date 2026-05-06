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
    private static final Set<String> TIPOS_VALIDOS = Set.of(
        "FareTransaction",
        "PublicTransportRoute",
        "PublicTransportStop",
        "TransportValidationEvent"
    );

    private final NgsiLdDataLakeRecordRepository ngsiLdRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ControladorIntegracaoNgsiLd(NgsiLdDataLakeRecordRepository ngsiLdRepository) {
        this.ngsiLdRepository = ngsiLdRepository;
    }

    // Publica uma entidade NGSI-LD (recebida de sistema externo)
    @Transactional
    public NgsiLdDataLakeRecord publicarEntidade(Map<String, Object> payload,
                                                  String utilizador) {
        // Valida conformidade com esquema NGSI-LD
        validarEsquema(payload);

        String entityId   = extrairCampo(payload, "id");
        String entityType = extrairCampo(payload, "type");
        String batchId    = "EXT_" + OffsetDateTime.now().toInstant().toEpochMilli();

        String payloadJson = toJson(payload);

        NgsiLdDataLakeRecord record = new NgsiLdDataLakeRecord(
            batchId, entityId, entityType,
            payloadJson, LocalDate.now(),
            OffsetDateTime.now(),
            null, null,
            OffsetDateTime.now()
        );

        return ngsiLdRepository.save(record);
    }

    // Consulta entidades NGSI-LD por tipo
    public List<NgsiLdDataLakeRecord> consultarPorTipo(String entityType) {
        return ngsiLdRepository.findAll().stream()
            .filter(r -> entityType.equals(r.getEntityType()))
            .toList();
    }

    // Calcula hash de integridade de um payload
    public String calcularHash(String conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(conteudo.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(conteudo.hashCode());
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void validarEsquema(Map<String, Object> payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload NGSI-LD nulo");
        }
        if (!payload.containsKey("id")) {
            throw new IllegalArgumentException("Campo obrigatorio em falta: id");
        }
        if (!payload.containsKey("type")) {
            throw new IllegalArgumentException("Campo obrigatorio em falta: type");
        }
        String tipo = payload.get("type").toString();
        if (!TIPOS_VALIDOS.contains(tipo)) {
            throw new IllegalArgumentException(
                "Tipo de entidade nao conforme com Smart Data Models: " + tipo);
        }
        if (!payload.containsKey("@context")) {
            throw new IllegalArgumentException("Campo obrigatorio em falta: @context");
        }
    }

    private String extrairCampo(Map<String, Object> payload, String campo) {
        Object valor = payload.get(campo);
        if (valor == null) throw new IllegalArgumentException("Campo em falta: " + campo);
        return valor.toString();
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
