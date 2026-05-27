// UC03.2 — Políticas RGPD e Direito ao Esquecimento
import { apiGet, apiPost, apiPut, apiDelete } from './apiClient';

export const getPoliticas   = ()         => apiGet('/api/rgpd/politicas');
export const createPolitica = (data)     => apiPost('/api/rgpd/politicas', data);
export const updatePolitica = (id, data) => apiPut(`/api/rgpd/politicas/${id}`, data);
export const deletePolitica = (id)       => apiDelete(`/api/rgpd/politicas/${id}`);

// Exportações que requerem aprovação do DPO
export const getExportacoes = () => apiGet('/api/ngsi-ld/exportacoes');

export const decisaoExportacao = (id, decisao) =>
  apiPut(`/api/ngsi-ld/exportacoes/${id}/aprovar`, { decisao });

// Direito ao Esquecimento
export const direitoAoEsquecimento = (cardId) =>
  apiPost('/api/rgpd/direito-ao-esquecimento', { cardId });
