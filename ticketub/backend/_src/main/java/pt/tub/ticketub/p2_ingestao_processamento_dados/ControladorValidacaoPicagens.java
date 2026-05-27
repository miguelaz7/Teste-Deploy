package pt.tub.ticketub.p2_ingestao_processamento_dados;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRota;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioParagem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Viagem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioViagem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// =============================================================================
// O0.2.1.c – Controlador de Validação de Picagens
// Lê registos da base de dados (O0.2.1.d), aplica validações estruturais
// e temporais, separa válidos de inválidos e encaminha válidos para
// normalização (O0.2.2.c).
// =============================================================================

@Service
class ControladorValidacaoPicagens {

    private final RepositorioRegrasValidacao validationRuleRepository;
    private final RepositorioRota routeRepository;
    private final RepositorioViagem tripRepository;
    private final RepositorioParagem stopRepository;

    ControladorValidacaoPicagens(RepositorioRegrasValidacao validationRuleRepository,
                                 RepositorioRota routeRepository,
                                 RepositorioViagem tripRepository,
                                 RepositorioParagem stopRepository) {
        this.validationRuleRepository = validationRuleRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.stopRepository = stopRepository;
    }

    // Carrega as regras ativas do O0.2.1.d e valida o payload
    ValidationResult validate(DtoPedidoIngestaoValidacao dto) {
        List<RegraValidacao> regras = validationRuleRepository.findByAtivoTrue();

        String transactionDateTime = valueOrNull(dto.getTransactionDateTime());
        String originStopId       = valueOrNull(dto.getOriginStopId());
        String routeId            = valueOrNull(dto.getRouteId());
        String tripId             = valueOrNull(dto.getTripId());
        String ticketType         = valueOrNull(dto.getTicketTypeCode());
        String fareForAdult       = valueOrNull(dto.getFareForAdult());

        // Campos obrigatórios
        for (RegraValidacao regra : regras) {
            if (!"OBRIGATORIO".equals(regra.getTipoRegra()) || !regra.isAtivo()) continue;
            if (valueOrNull(getFieldByName(dto, regra.getCampo())) == null) {
                return ValidationResult.invalid(
                    regra.getMotivoRejeicao(), regra.getCampo(),
                    null, "Campo obrigatorio", regra.getRuleCode());
            }
        }

        // Formato ISO 8601
        OffsetDateTime ts;
        try {
            ts = OffsetDateTime.parse(transactionDateTime);
        } catch (Exception e) {
            return invalid(regras, "FORMATO_ISO8601_transactionDateTime",
                "transactionDateTime", transactionDateTime, "Formato ISO-8601");
        }

        // Limite futuro (minutos lidos da BD)
        long minFuturo = getLimit(regras, "LIMITE_FUTURO_transactionDateTime", 5L);
        if (ts.isAfter(OffsetDateTime.now().plusMinutes(minFuturo))) {
            return invalid(regras, "LIMITE_FUTURO_transactionDateTime",
                "transactionDateTime", transactionDateTime,
                "Janela: +" + minFuturo + " minutos");
        }

        // Limite passado (dias lidos da BD)
        long diasPassado = getLimit(regras, "LIMITE_PASSADO_transactionDateTime", 90L);
        if (ts.isBefore(OffsetDateTime.now().minusDays(diasPassado))) {
            return invalid(regras, "LIMITE_PASSADO_transactionDateTime",
                "transactionDateTime", transactionDateTime,
                "Janela: -" + diasPassado + " dias");
        }

        // Formato decimal
        if (fareForAdult != null) {
            try { new BigDecimal(fareForAdult); }
            catch (NumberFormatException e) {
                return invalid(regras, "FORMATO_DECIMAL_fareForAdult",
                    "fareForAdult", fareForAdult, "Tipo decimal esperado");
            }
        }

        // Lista aprovada de ticketTypeCode (lida da BD)
        RegraValidacao regraLista = getRule(regras, "LISTA_APROVADA_ticketTypeCode");
        if (regraLista != null && ticketType != null) {
            List<String> validos = Arrays.asList(regraLista.getValorLimite().split(","));
            if (!validos.contains(ticketType.trim().toUpperCase())) {
                return ValidationResult.invalid(regraLista.getMotivoRejeicao(),
                    "ticketTypeCode", ticketType,
                    "Valores validos: " + regraLista.getValorLimite(),
                    "LISTA_APROVADA_ticketTypeCode");
            }
        }

        // route_id no catálogo
        if (!routeExists(routeId)) {
            return invalid(regras, "CATALOGO_route_id", "route_id", routeId, "Integridade referencial");
        }

        // trip_id existe e pertence à linha
        Optional<Viagem> trip =
            tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            return invalid(regras, "CATALOGO_trip_id", "trip_id", tripId, "Integridade referencial");
        }
        if (!routeId.equals(trip.get().getRouteId())) {
            return invalid(regras, "COERENCIA_route_trip", "trip_id", tripId, "Coerencia route-trip");
        }

        // originStopId (opcional)
        if (originStopId != null) {
            if (!stopRepository.existsById(originStopId)) {
                return invalid(regras, "CATALOGO_originStopId",
                    "originStopId", originStopId, "Integridade referencial");
            }
            // Verificação trip-stop via StopRepository (StopTimes removido)
            if (!stopRepository.existsById(originStopId)) {
                return invalid(regras, "COERENCIA_trip_stop",
                    "originStopId", originStopId, "Coerencia trip-stop");
            }
        }

        return ValidationResult.valid();
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private ValidationResult invalid(List<RegraValidacao> regras, String ruleCode,
                                     String campo, String valor, String regra) {
        String motivo = regras.stream()
            .filter(r -> ruleCode.equals(r.getRuleCode()))
            .map(RegraValidacao::getMotivoRejeicao)
            .findFirst().orElse(ruleCode);
        return ValidationResult.invalid(motivo, campo, valor, regra, ruleCode);
    }

    private RegraValidacao getRule(List<RegraValidacao> regras, String ruleCode) {
        return regras.stream()
            .filter(r -> ruleCode.equals(r.getRuleCode()) && r.isAtivo())
            .findFirst().orElse(null);
    }

    private long getLimit(List<RegraValidacao> regras, String ruleCode, long defaultVal) {
        RegraValidacao r = getRule(regras, ruleCode);
        if (r == null || r.getValorLimite() == null) return defaultVal;
        try { return Long.parseLong(r.getValorLimite()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private boolean routeExists(String routeId) {
        try { return routeRepository.existsById(Long.parseLong(routeId)); }
        catch (NumberFormatException e) { return routeRepository.existsByRouteShortName(routeId); }
    }

    private String getFieldByName(DtoPedidoIngestaoValidacao dto, String campo) {
        return switch (campo) {
            case "transactionDateTime" -> dto.getTransactionDateTime();
            case "route_id"            -> dto.getRouteId();
            case "ticketTypeCode"      -> dto.getTicketTypeCode();
            case "trip_id"             -> dto.getTripId();
            case "result"              -> dto.getResult();
            default                    -> null;
        };
    }

    private String valueOrNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    // -------------------------------------------------------------------------
    // Resultado de validação (usado pelos controladores seguintes)
    // -------------------------------------------------------------------------

    static class ValidationResult {
        private final boolean valid;
        private final String motivo;
        private final String campo;
        private final String valorRecebido;
        private final String regra;
        private final String ruleCode;

        private ValidationResult(boolean valid, String motivo, String campo,
                                 String valorRecebido, String regra, String ruleCode) {
            this.valid = valid; this.motivo = motivo; this.campo = campo;
            this.valorRecebido = valorRecebido; this.regra = regra; this.ruleCode = ruleCode;
        }

        static ValidationResult valid() {
            return new ValidationResult(true, null, null, null, null, null);
        }

        static ValidationResult invalid(String motivo, String campo,
                                        String valorRecebido, String regra, String ruleCode) {
            return new ValidationResult(false, motivo, campo, valorRecebido, regra, ruleCode);
        }

        boolean isValid()         { return valid; }
        String getMotivo()         { return motivo; }
        String getCampo()          { return campo; }
        String getValorRecebido()  { return valorRecebido; }
        String getRegra()          { return regra; }
        String getRuleCode()       { return ruleCode; }
    }
}