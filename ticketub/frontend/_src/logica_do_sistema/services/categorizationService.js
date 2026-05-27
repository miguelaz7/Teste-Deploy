import { apiGet, apiPost, apiPut, apiDelete } from './apiClient';

const BASE = '/categorization';

export const getCategorizationStats      = () => apiGet(`${BASE}/stats`);
export const getMappings                 = () => apiGet(`${BASE}/mappings`);
export const createMapping               = (data) => apiPost(`${BASE}/mappings`, { tipoTitulo: data.tipo_titulo, perfil: data.perfil });
export const updateMapping               = (id, data) => apiPut(`${BASE}/mappings/${id}`, { tipoTitulo: data.tipo_titulo, perfil: data.perfil });
export const deleteMapping               = (id) => apiDelete(`${BASE}/mappings/${id}`);
export const getUncategorizedEvents      = (dataInicio, dataFim) => {
  const params = new URLSearchParams();
  if (dataInicio) params.append('dataInicio', dataInicio);
  if (dataFim)    params.append('dataFim', dataFim);
  const qs = params.toString() ? `?${params.toString()}` : '';
  return apiGet(`${BASE}/uncategorized${qs}`);
};
export const deleteUncategorizedEvents   = () => apiDelete(`${BASE}/uncategorized`);
export const reprocessEvents             = () => apiPost(`${BASE}/reprocess`, {});
export const resetEvents                 = () => apiPost(`${BASE}/reset`, {});
export const logAuditAction              = (acao, tipoTitulo, perfilAnterior, perfilNovo, utilizador) =>
  apiPost(`${BASE}/audit`, { acao, tipoTitulo, perfilAnterior, perfilNovo, utilizador: utilizador || 'sistema' });
export const getPendingNaoCategorizados  = () => apiGet(`${BASE}/nao-categorizados`);
export const resolverNaoCategorizado     = (id, estado, resolvidoPor = 'admin') =>
  apiPut(`${BASE}/nao-categorizados/${id}/resolver`, { estado, resolvidoPor });
