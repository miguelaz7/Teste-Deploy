export const API_BASE_URL = 'http://localhost:8080/api/od';

const fetchOD = async (endpoint) => {
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

export const getFluxos = () => fetchOD('/fluxos');
export const getDadosAbertos = () => fetchOD('/dados-abertos');

export const downloadExportacao = (fileName = 'matriz-od-export.csv') => {
  // Num cenário real faríamos:
  // fetch(`${API_BASE_URL}/exportar`) -> const blob = await res.blob()
  // Mas como podemos estar num mock, abrimos uma tab para o download ou simulamos.
  
  // Exemplo de como abriríamos o endpoint de download diretamente no browser:
  // window.open(`${API_BASE_URL}/exportar`, '_blank');
  
  // Mas para não quebrar a UI se não houver backend, criamos um Blob com dummy data temporário:
  const csvContent = "origem,destino,volume,periodo\nP1,P2,150,MANHA\nP3,P4,80,TARDE";
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', fileName);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};
