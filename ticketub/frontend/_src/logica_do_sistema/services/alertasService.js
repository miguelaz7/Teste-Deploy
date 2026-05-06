export const API_BASE_URL = 'http://localhost:8080/api/alertas';

const fetchAlertas = async (endpoint) => {
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

export const getAlertasAtivos = () => fetchAlertas('/ativos');
export const getQuarentena = () => fetchAlertas('/quarentena');
export const getAlertasPorLinha = () => fetchAlertas('/por-linha');
