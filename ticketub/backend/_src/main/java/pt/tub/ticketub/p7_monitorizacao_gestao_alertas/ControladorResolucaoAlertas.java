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

    private final RepositorioAlerta alertaRepository;

    public ControladorResolucaoAlertas(RepositorioAlerta alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    // Lista alertas por estado
    public List<Alerta> listByStatus(String status) {
        return alertaRepository.findByStatus(status);
    }

    // Atribui um alerta a um técnico ou equipa
    @Transactional
    public Alerta assign(Long id, String assignedTo) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
        alerta.setStatus("EM_ANALISE");
        alerta.setAssignedTo(assignedTo);
        return alertaRepository.save(alerta);
    }

    // Resolve um alerta com acção registada
    @Transactional
    public Alerta resolve(Long id, String resolutionAction) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
        alerta.setStatus("RESOLVIDO");
        alerta.setResolutionAction(resolutionAction);
        alerta.setResolvedAt(OffsetDateTime.now());
        return alertaRepository.save(alerta);
    }

    // Marca como falso positivo e verifica se deve escalar revisão de regras
    @Transactional
    public Map<String, Object> markFalsePositive(Long id, String resolutionAction) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta nao encontrado: " + id));
        alerta.setStatus("FALSO_POSITIVO");
        alerta.setResolutionAction(resolutionAction);
        alerta.setResolvedAt(OffsetDateTime.now());
        alertaRepository.save(alerta);

        // Verificar se falsos positivos recorrentes do mesmo tipo exigem revisão
        long totalFalsos = alertaRepository
            .findByTypeAndStatus(alerta.getType(), "FALSO_POSITIVO").size();

        boolean revisaoNecessaria = totalFalsos >= LIMIAR_FALSOS_POSITIVOS;

        return Map.of(
            "estado", "FALSO_POSITIVO",
            "tipo", alerta.getType(),
            "totalFalsosPositivos", totalFalsos,
            "revisaoRegrasNecessaria", revisaoNecessaria,
            "mensagem", revisaoNecessaria
                ? "ATENCAO: " + totalFalsos + " falsos positivos do tipo "
                    + alerta.getType() + ". Revisao de regras recomendada."
                : "Falso positivo registado."
        );
    }

    // Resumo de alertas activos por severidade
    public Map<String, Object> getSummary() {
        long criticos  = alertaRepository.countByStatusAndCreatedAtAfter(
            "PENDENTE", OffsetDateTime.now().minusHours(24));
        long pendentes = alertaRepository.findByStatus("PENDENTE").size();
        long emAnalise = alertaRepository.findByStatus("EM_ANALISE").size();

        return Map.of(
            "pendentes",   pendentes,
            "emAnalise",   emAnalise,
            "criticos24h", criticos
        );
    }
}
