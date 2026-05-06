package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

// =============================================================================
// O0.9.2.c – Controlador de Resolução de Alertas
// Coordena atribuição de alertas, regista acções de resolução e actualiza
// o estado. Falsos positivos recorrentes geram pedido de revisão de regras.
// Consome dados de: O0.9.1.d (AlertaRepository).
// =============================================================================

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ControladorResolucaoAlertas {

    // Limiar de falsos positivos que gera pedido de revisão de regras
    private static final long LIMIAR_FALSOS_POSITIVOS = 5;

    private final AlertaRepository alertaRepository;

    public ControladorResolucaoAlertas(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    // Lista alertas por estado
    public List<Alerta> listarPorEstado(String estado) {
        return alertaRepository.findByEstado(estado);
    }

    // Atribui um alerta a um técnico ou equipa
    @Transactional
    public Alerta atribuir(Long id, String atribuidoA) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
        alerta.setEstado("EM_ANALISE");
        alerta.setAtribuidoA(atribuidoA);
        return alertaRepository.save(alerta);
    }

    // Resolve um alerta com acção registada
    @Transactional
    public Alerta resolver(Long id, String accaoResolucao) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
        alerta.setEstado("RESOLVIDO");
        alerta.setAccaoResolucao(accaoResolucao);
        alerta.setResolvidoEm(OffsetDateTime.now());
        return alertaRepository.save(alerta);
    }

    // Marca como falso positivo e verifica se deve escalar revisão de regras
    @Transactional
    public Map<String, Object> marcarFalsoPositivo(Long id, String accaoResolucao) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
        alerta.setEstado("FALSO_POSITIVO");
        alerta.setAccaoResolucao(accaoResolucao);
        alerta.setResolvidoEm(OffsetDateTime.now());
        alertaRepository.save(alerta);

        // Verificar se falsos positivos recorrentes do mesmo tipo exigem revisão
        long totalFalsos = alertaRepository
            .findByTipoAndEstado(alerta.getTipo(), "FALSO_POSITIVO").size();

        boolean revisaoNecessaria = totalFalsos >= LIMIAR_FALSOS_POSITIVOS;

        return Map.of(
            "estado", "FALSO_POSITIVO",
            "tipo", alerta.getTipo(),
            "totalFalsosPositivos", totalFalsos,
            "revisaoRegrasNecessaria", revisaoNecessaria,
            "mensagem", revisaoNecessaria
                ? "ATENCAO: " + totalFalsos + " falsos positivos do tipo "
                    + alerta.getTipo() + ". Revisao de regras recomendada."
                : "Falso positivo registado."
        );
    }

    // Resumo de alertas activos por severidade
    public Map<String, Object> obterResumo() {
        long criticos  = alertaRepository.countByEstadoAndCriadoEmAfter(
            "PENDENTE", OffsetDateTime.now().minusHours(24));
        long pendentes = alertaRepository.findByEstado("PENDENTE").size();
        long emAnalise = alertaRepository.findByEstado("EM_ANALISE").size();

        return Map.of(
            "pendentes",   pendentes,
            "emAnalise",   emAnalise,
            "criticos24h", criticos
        );
    }
}
