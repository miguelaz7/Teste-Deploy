export const API_BASE_URL = 'http://localhost:8080/api/planeamento';

const fetchPlaneamento = async (endpoint, method = 'GET', body = null) => {
  try {
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
      },
      cache: 'no-store'
    };

    if (body) {
      options.body = JSON.stringify(body);
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, options);

    if (!response.ok) {
      throw new Error(`Erro ${response.status} ao contactar ${endpoint}`);
    }

    return await response.json();
  } catch (error) {
    console.error(`Falha no fetch para ${endpoint}:`, error);
    throw error;
  }
};

export const submeterSimulacao = (payload) => fetchPlaneamento('/simular', 'POST', payload);
export const getCenarios = () => fetchPlaneamento('/cenarios');
export const getProjecao = () => fetchPlaneamento('/projecao');
export const getDadosFinanceiros = () => fetchPlaneamento('/dados-financeiros');
export const gerarParaERP = (payload) => fetchPlaneamento('/erp/gerar', 'POST', payload);
