const API_BASE_URL = (process.env.REACT_APP_API_URL || 'http://localhost:8080') + '/categorization';

export const getCategorizationStats = async () => {
    const response = await fetch(`${API_BASE_URL}/stats`);
    if (!response.ok) throw new Error('Failed to fetch categorization stats');
    return response.json();
};

export const getMappings = async () => {
    const response = await fetch(`${API_BASE_URL}/mappings`);
    if (!response.ok) throw new Error('Failed to fetch mappings');
    return response.json();
};

export const createMapping = async (data) => {
    const response = await fetch(`${API_BASE_URL}/mappings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tipoTitulo: data.tipo_titulo, perfil: data.perfil })
    });
    if (!response.ok) throw new Error('Failed to create mapping');
    return response.json();
};

export const updateMapping = async (id, data) => {
    const response = await fetch(`${API_BASE_URL}/mappings/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ tipoTitulo: data.tipo_titulo, perfil: data.perfil })
    });
    if (!response.ok) throw new Error('Failed to update mapping');
    return response.json();
};

export const deleteMapping = async (id) => {
    const response = await fetch(`${API_BASE_URL}/mappings/${id}`, { method: 'DELETE' });
    if (!response.ok) throw new Error('Failed to delete mapping');
    return true;
};

export const getUncategorizedEvents = async (dataInicio, dataFim) => {
    let url = `${API_BASE_URL}/uncategorized`;
    const params = new URLSearchParams();
    if (dataInicio) params.append('dataInicio', dataInicio);
    if (dataFim)    params.append('dataFim', dataFim);
    if (params.toString()) url += `?${params.toString()}`;

    const response = await fetch(url);
    if (!response.ok) throw new Error('Failed to fetch uncategorized events');
    return response.json();
};

export const deleteUncategorizedEvents = async () => {
    const response = await fetch(`${API_BASE_URL}/uncategorized`, {
        method: 'DELETE'
    });
    if (!response.ok) throw new Error('Failed to delete uncategorized events');
    return true;
};

export const reprocessEvents = async () => {
    const response = await fetch(`${API_BASE_URL}/reprocess`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    });
    if (!response.ok) throw new Error('Failed to reprocess events');
    return response.json();
};

export const resetEvents = async () => {
    const response = await fetch(`${API_BASE_URL}/reset`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    });
    if (!response.ok) throw new Error('Failed to reset system');
    return response.json();
};

export const logAuditAction = async (acao, tipoTitulo, perfilAnterior, perfilNovo, utilizador) => {
    const response = await fetch(`${API_BASE_URL}/audit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ acao, tipoTitulo, perfilAnterior, perfilNovo, utilizador: utilizador || 'sistema' })
    });
    if (!response.ok) throw new Error('Failed to log audit action');
    return response.json();
};
