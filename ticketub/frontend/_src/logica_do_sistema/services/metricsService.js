/**
 * Servico centralizado para buscar as metricas do dashboard.
 * Segue a regra do AGENTS.md: separacao da logica de fetching dos componentes.
 */

const API_BASE_URL = 'http://localhost:8080/api/dashboard';

export const getDashboardMetrics = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/metricas-ingestao`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store'
    });

    if (!response.ok) {
      throw new Error(`Erro na API (${response.status}) ao buscar métricas.`);
    }

    return await response.json();
  } catch (error) {
    console.error('Falha ao obter dashboard metrics:', error);
    throw error;
  }
};

