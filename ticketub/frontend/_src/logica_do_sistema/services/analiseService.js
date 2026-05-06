export const API_BASE_URL = 'http://localhost:8080/api/analise';

/**
 * Função utilitária genérica para fetch
 */
const fetchAnalise = async (endpoint) => {
  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store'
    });

    if (!response.ok) {
      throw new Error(`Erro ${response.status} ao obter dados de ${endpoint}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`Falha no fetch para ${endpoint}:`, error);
    throw error;
  }
};

// -- Endpoints de Tempo Real --

export const getTempoReal = () => fetchAnalise('/tempo-real');
export const getPorHorario = () => fetchAnalise('/por-horario');
export const getPorLinha = () => fetchAnalise('/por-linha');
export const getPorParagem = () => fetchAnalise('/por-paragem');

// -- Endpoints de Histórico Consolidado --

// Note: assuming /metricas-ingestao in analise backend too. If it's the dashboard one, it might be different, but for now we'll put it here.
export const getMetricasIngestao = () => fetchAnalise('/metricas-ingestao');
export const getComparacaoPeriodos = (inicio, fim) => fetchAnalise(`/comparacao-periodos?inicio=${inicio}&fim=${fim}`);
