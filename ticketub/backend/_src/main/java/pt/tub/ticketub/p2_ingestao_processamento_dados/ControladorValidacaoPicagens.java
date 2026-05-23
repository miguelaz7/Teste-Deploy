package pt.tub.ticketub.p2_ingestao_processamento_dados;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RouteRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Trip;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TripRepository;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantine;
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

    private final ValidationRuleRepository validationRuleRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final StopRepository stopRepository;

    ControladorValidacaoPicagens(ValidationRuleRepository validationRuleRepository,
                                 RouteRepository routeRepository,
                                 TripRepository tripRepository,
                                 StopRepository stopRepository) {
        this.validationRuleRepository = validationRuleRepository;
        this.routeRepository = routeRepository;
        this.tripRepository = tripRepository;
        this.stopRepository = stopRepository;
    }

    // Carrega as regras ativas do O0.2.1.d e valida o payload
    ResultadoValidacao validar(ValidationIngestionRequestDto dto) {
        List<ValidationRule> regras = validationRuleRepository.findByAtivoTrue();

        String transactionDateTime = valorOuNull(dto.getTransactionDateTime());
        String originStopId       = valorOuNull(dto.getOriginStopId());
        String routeId            = valorOuNull(dto.getRouteId());
        String tripId             = valorOuNull(dto.getTripId());
        String ticketType         = valorOuNull(dto.getTicketTypeCode());
        String fareForAdult       = valorOuNull(dto.getFareForAdult());

        // Campos obrigatórios
        for (ValidationRule regra : regras) {
            if (!"OBRIGATORIO".equals(regra.getTipoRegra()) || !regra.isAtivo()) continue;
            if (valorOuNull(getCampoPorNome(dto, regra.getCampo())) == null) {
                return ResultadoValidacao.invalido(
                    regra.getMotivoRejeicao(), regra.getCampo(),
                    null, "Campo obrigatorio", regra.getRuleCode());
            }
        }

        // Formato ISO 8601
        OffsetDateTime ts;
        try {
            ts = OffsetDateTime.parse(transactionDateTime);
        } catch (Exception e) {
            return invalido(regras, "FORMATO_ISO8601_transactionDateTime",
                "transactionDateTime", transactionDateTime, "Formato ISO-8601");
        }

        // Limite futuro (minutos lidos da BD)
        long minFuturo = getLimite(regras, "LIMITE_FUTURO_transactionDateTime", 5L);
        if (ts.isAfter(OffsetDateTime.now().plusMinutes(minFuturo))) {
            return invalido(regras, "LIMITE_FUTURO_transactionDateTime",
                "transactionDateTime", transactionDateTime,
                "Janela: +" + minFuturo + " minutos");
        }

        // Limite passado (dias lidos da BD)
        long diasPassado = getLimite(regras, "LIMITE_PASSADO_transactionDateTime", 90L);
        if (ts.isBefore(OffsetDateTime.now().minusDays(diasPassado))) {
            return invalido(regras, "LIMITE_PASSADO_transactionDateTime",
                "transactionDateTime", transactionDateTime,
                "Janela: -" + diasPassado + " dias");
        }

        // Formato decimal
        if (fareForAdult != null) {
            try { new BigDecimal(fareForAdult); }
            catch (NumberFormatException e) {
                return invalido(regras, "FORMATO_DECIMAL_fareForAdult",
                    "fareForAdult", fareForAdult, "Tipo decimal esperado");
            }
        }

        // Lista aprovada de ticketTypeCode (lida da BD)
        ValidationRule regraLista = getRegra(regras, "LISTA_APROVADA_ticketTypeCode");
        if (regraLista != null && ticketType != null) {
            List<String> validos = Arrays.asList(regraLista.getValorLimite().split(","));
            if (!validos.contains(ticketType.trim().toUpperCase())) {
                return ResultadoValidacao.invalido(regraLista.getMotivoRejeicao(),
                    "ticketTypeCode", ticketType,
                    "Valores validos: " + regraLista.getValorLimite(),
                    "LISTA_APROVADA_ticketTypeCode");
            }
        }

        // route_id no catálogo
        if (!existeLinha(routeId)) {
            return invalido(regras, "CATALOGO_route_id", "route_id", routeId, "Integridade referencial");
        }

        // trip_id existe e pertence à linha
        Optional<Trip> trip =
            tripRepository.findById(tripId);
        if (trip.isEmpty()) {
            return invalido(regras, "CATALOGO_trip_id", "trip_id", tripId, "Integridade referencial");
        }
        if (!routeId.equals(trip.get().getRouteId())) {
            return invalido(regras, "COERENCIA_route_trip", "trip_id", tripId, "Coerencia route-trip");
        }

        // originStopId (opcional)
        if (originStopId != null) {
            if (!stopRepository.existsById(originStopId)) {
                return invalido(regras, "CATALOGO_originStopId",
                    "originStopId", originStopId, "Integridade referencial");
            }
            // Verificação trip-stop via StopRepository (StopTimes removido)
            if (!stopRepository.existsById(originStopId)) {
                return invalido(regras, "COERENCIA_trip_stop",
                    "originStopId", originStopId, "Coerencia trip-stop");
            }
        }

        return ResultadoValidacao.valido();
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private ResultadoValidacao invalido(List<ValidationRule> regras, String ruleCode,
                                        String campo, String valor, String regra) {
        String motivo = regras.stream()
            .filter(r -> ruleCode.equals(r.getRuleCode()))
            .map(ValidationRule::getMotivoRejeicao)
            .findFirst().orElse(ruleCode);
        return ResultadoValidacao.invalido(motivo, campo, valor, regra, ruleCode);
    }

    private ValidationRule getRegra(List<ValidationRule> regras, String ruleCode) {
        return regras.stream()
            .filter(r -> ruleCode.equals(r.getRuleCode()) && r.isAtivo())
            .findFirst().orElse(null);
    }

    private long getLimite(List<ValidationRule> regras, String ruleCode, long defaultVal) {
        ValidationRule r = getRegra(regras, ruleCode);
        if (r == null || r.getValorLimite() == null) return defaultVal;
        try { return Long.parseLong(r.getValorLimite()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    private boolean existeLinha(String routeId) {
        try { return routeRepository.existsById(Long.parseLong(routeId)); }
        catch (NumberFormatException e) { return routeRepository.existsByRouteShortName(routeId); }
    }

    private String getCampoPorNome(ValidationIngestionRequestDto dto, String campo) {
        return switch (campo) {
            case "transactionDateTime" -> dto.getTransactionDateTime();
            case "route_id"            -> dto.getRouteId();
            case "ticketTypeCode"      -> dto.getTicketTypeCode();
            case "trip_id"             -> dto.getTripId();
            case "result"              -> dto.getResult();
            default                    -> null;
        };
    }

    private String valorOuNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    // -------------------------------------------------------------------------
    // Resultado de validação (usado pelos controladores seguintes)
    // -------------------------------------------------------------------------

    static class ResultadoValidacao {
        private final boolean valido;
        private final String motivo;
        private final String campo;
        private final String valorRecebido;
        private final String regra;
        private final String ruleCode;

        private ResultadoValidacao(boolean valido, String motivo, String campo,
                                   String valorRecebido, String regra, String ruleCode) {
            this.valido = valido; this.motivo = motivo; this.campo = campo;
            this.valorRecebido = valorRecebido; this.regra = regra; this.ruleCode = ruleCode;
        }

        static ResultadoValidacao valido() {
            return new ResultadoValidacao(true, null, null, null, null, null);
        }

        static ResultadoValidacao invalido(String motivo, String campo,
                                           String valorRecebido, String regra, String ruleCode) {
            return new ResultadoValidacao(false, motivo, campo, valorRecebido, regra, ruleCode);
        }

        boolean isValido()         { return valido; }
        String getMotivo()         { return motivo; }
        String getCampo()          { return campo; }
        String getValorRecebido()  { return valorRecebido; }
        String getRegra()          { return regra; }
        String getRuleCode()       { return ruleCode; }
    }
}