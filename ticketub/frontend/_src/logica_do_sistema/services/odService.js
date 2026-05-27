// UC08 — Origem-Destino → /api/od/*
// UC12 — Exportação     → /api/exportacao/*
import { apiGet, apiPost } from './apiClient';

export const getFluxos       = () => apiGet('/api/od/fluxos');
export const getDadosAbertos = () => apiGet('/api/exportacao/dados-abertos');
export const exportarMatrizOD = (dataInicio, dataFim, formato) => apiGet(`/api/od/exportar?dataInicio=${dataInicio || ''}&dataFim=${dataFim || ''}&formato=${formato || 'CSV'}`);

export const getExportacoes = () => apiGet('/api/ngsi-ld/exportacoes');

export const decisaoExportacao = (id, decisao) =>
  apiPost(`/api/ngsi-ld/exportacoes/${id}/aprovar`, { decisao });

export const downloadExportacao = (fileName = 'matriz-od-export.csv') => {
  const csv = 'origem,destino,volume,periodo\nP1,P2,150,MANHA\nP3,P4,80,TARDE';
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.setAttribute('download', fileName);
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
};
