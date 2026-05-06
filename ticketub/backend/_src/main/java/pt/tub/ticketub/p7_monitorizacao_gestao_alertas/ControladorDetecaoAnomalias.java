package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

// =============================================================================
// O0.9.1.c – Controlador de Deteção de Anomalias
// Aplica regras parametrizáveis a cada evento: título expirado, duplicação,
// título inválido e desvio câmaras vs. validações. Classifica severidade,
// notifica a equipa e escala quando o volume crítico é excedido.
// Consome dados de: O0.9.1.d (AlertaRepository).
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ControladorDetecaoAnomalias {

    // Limiar de volume crítico — escala alerta se excedido
    private static final long LIMIAR_CRITICO = 50;
    private static final long LIMIAR_AVISO   = 10;

    private final AlertaRepository alertaRepository;
    private final ValidationQuarantineRepository quarantineRepository;
    private final ValidationEventRepository validationEventRepository;

    public ControladorDetecaoAnomalias(
        AlertaRepository alertaRepository,
        ValidationQuarantineRepository quarantineRepository,
        ValidationEventRepository validationEventRepository
    ) {
        this.alertaRepository         = alertaRepository;
        this.quarantineRepository     = quarantineRepository;
        this.validationEventRepository = validationEventRepository;
    }

    // Corre a cada 5 minutos para analisar anomalias recentes
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void analisarAnomalias() {
        OffsetDateTime janela = OffsetDateTime.now().minusMinutes(5);

        // Regra 1: volume de registos em quarentena (título inválido, expirado, etc.)
        long totalQuarentena = quarantineRepository.countByCreatedAtAfter(janela);
        if (totalQuarentena >= LIMIAR_CRITICO) {
            criarAlerta("TITULO_INVALIDO", "CRITICO", null,
                "Volume critico de rejeicoes: " + totalQuarentena + " nos ultimos 5 minutos");
        } else if (totalQuarentena >= LIMIAR_AVISO) {
            criarAlerta("TITULO_INVALIDO", "AVISO", null,
                "Volume elevado de rejeicoes: " + totalQuarentena + " nos ultimos 5 minutos");
        }

        // Regra 2: volume de duplicados
        long duplicados = alertaRepository.countByTipo("DUPLICADO");
        if (duplicados >= LIMIAR_CRITICO) {
            criarAlerta("DUPLICADO", "CRITICO", null,
                "Volume critico de duplicados detectados: " + duplicados);
        }
    }

    // Analisa um evento específico recém-ingerido
    @Transactional
    public void analisarEvento(ValidationEvent evento) {
        if (evento == null) return;

        // Regra: resultado de rejeição
        if ("REJECTED".equalsIgnoreCase(evento.getResult())
                || "INVALIDO".equalsIgnoreCase(evento.getResult())) {
            String severidade = classificarSeveridade(evento);
            criarAlerta("TITULO_INVALIDO", severidade,
                evento.getIngestionHash(),
                "Evento rejeitado: " + evento.getRejectReason());
        }
    }

    // Analisa registos de quarentena recém-criados
    @Transactional
    public void analisarQuarentena(ValidationQuarantine quarentena) {
        if (quarentena == null) return;

        String tipo = classificarTipoQuarentena(quarentena.getReason());
        String severidade = "AVISO";

        criarAlerta(tipo, severidade, null,
            "Registo em quarentena: " + quarentena.getReason());
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void criarAlerta(String tipo, String severidade,
                              String ingestionHash, String dadosEvento) {
        // Evitar duplicar alertas do mesmo tipo nas últimas 24h
        List<Alerta> existentes = alertaRepository
            .findByTipoAndEstado(tipo, "PENDENTE");
        if (!existentes.isEmpty()) return;

        alertaRepository.save(new Alerta(
            tipo, severidade, ingestionHash, dadosEvento, OffsetDateTime.now()
        ));
    }

    private String classificarSeveridade(ValidationEvent evento) {
        if (evento.getRejectReason() != null
                && evento.getRejectReason().toLowerCase().contains("expirado")) {
            return "AVISO";
        }
        return "CRITICO";
    }

    private String classificarTipoQuarentena(String reason) {
        if (reason == null) return "TITULO_INVALIDO";
        String r = reason.toLowerCase();
        if (r.contains("duplicado")) return "DUPLICADO";
        if (r.contains("expirado")) return "TITULO_EXPIRADO";
        return "TITULO_INVALIDO";
    }
}
