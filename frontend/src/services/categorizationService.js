const API_BASE_URL = (process.env.REACT_APP_API_URL || 'http://localhost:8080') + '/api/categorization';

export const getCategorizationStats = async () => {
    const response = await fetch(`${API_BASE_URL}/stats`);
    if (!response.ok) {
        throw new Error('Failed to fetch categorization stats');
    }
    return response.json();
};

export const getMappings = async () => {
    const response = await fetch(`${API_BASE_URL}/mappings`);
    if (!response.ok) {
        throw new Error('Failed to fetch mappings');
    }
    return response.json();
};

export const createMapping = async (data) => {
    const response = await fetch(`${API_BASE_URL}/mappings`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    });
    if (!response.ok) {
        throw new Error('Failed to create mapping');
    }
    return response.json();
};

export const deleteMapping = async (id) => {
    const response = await fetch(`${API_BASE_URL}/mappings/${id}`, {
        method: 'DELETE'
    });
    if (!response.ok) {
        throw new Error('Failed to delete mapping');
    }
    return true;
};

export const getUncategorizedEvents = async (startDate, endDate) => {
    let url = `${API_BASE_URL}/uncategorized`;
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    if (params.toString()) {
        url += `?${params.toString()}`;
    }

    const response = await fetch(url);
    if (!response.ok) {
        throw new Error('Failed to fetch uncategorized events');
    }
    return response.json();
};

export const reprocessEvents = async (id) => {
    // Note: the backend reprocess endpoint doesn't currently take an ID, 
    // it reprocesses all, but we send it just in case for future granularity
    const response = await fetch(`${API_BASE_URL}/reprocess`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(id ? { id } : {})
    });
    if (!response.ok) {
        throw new Error('Failed to reprocess events');
    }
    return response.json();
};

export const logAuditAction = async (action, details) => {
    const response = await fetch(`${API_BASE_URL}/audit`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            acao: action,
            tipoTitulo: details.tipo_titulo || 'UNKNOWN',
            perfilAnterior: details.perfilAnterior || null,
            perfilNovo: details.perfil || null,
            utilizador: 'admin_user'
        })
    });
    if (!response.ok) {
        throw new Error('Failed to log audit action');
    }
    return response.json();
};
