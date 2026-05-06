export const API_BASE_URL = 'http://localhost:8080/api/politicas';

const fetchRGPD = async (endpoint = '', method = 'GET', body = null) => {
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

    // Para DELETE o backend pode não retornar JSON
    if (method === 'DELETE' || response.status === 204) {
      return true;
    }

    return await response.json();
  } catch (error) {
    console.error(`Falha no fetch RGPD para ${endpoint}:`, error);
    throw error;
  }
};

export const getPoliticas = () => fetchRGPD();
export const createPolitica = (data) => fetchRGPD('', 'POST', data);
export const updatePolitica = (id, data) => fetchRGPD(`/${id}`, 'PUT', data);
export const deletePolitica = (id) => fetchRGPD(`/${id}`, 'DELETE');
