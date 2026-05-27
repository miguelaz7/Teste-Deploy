// UC05 — Procura em Tempo Real → /api/procura/*
// UC10 — Histórico             → /api/historico/*
// UC04 — Dashboard KPIs        → /api/dashboard/*
import { apiGet, apiPost } from './apiClient';

export const getTempoReal           = () => apiGet('/api/procura/tempo-real');
export const getPorHorario          = () => apiGet('/api/procura/por-horario');
export const getPorLinha            = () => apiGet('/api/procura/por-linha');
export const getPorParagem          = () => apiGet('/api/procura/por-paragem');
export const getPorZona             = () => apiGet('/api/procura/por-zona');
export const getTop10Linhas         = () => apiGet('/api/procura/por-linha/top-10');
export const getDetalheHorarioLinha = (routeId) => apiGet(`/api/procura/por-linha/${routeId}/horario`);
export const getMetricasIngestao    = () => apiGet('/api/dashboard/metricas-ingestao');

export const getComparacaoPeriodos  = (inicio, fim, compInicio = '', compFim = '') => {
  let url = `/api/historico/comparacao-periodos?inicio=${inicio}&fim=${fim}`;
  if (compInicio && compFim) {
    url += `&compInicio=${compInicio}&compFim=${compFim}`;
  }
  return apiGet(url);
};

export const getSeriesTemporais = (params) => {
  const query = new URLSearchParams(params).toString();
  return apiGet(`/api/historico/series-temporais?${query}`);
};

export const exportarHistorico = (params) => {
  const query = new URLSearchParams(params).toString();
  return apiGet(`/api/historico/exportar?${query}`);
};

export const reprocessarPeriodo = (inicio, fim) => {
  return apiPost(`/api/historico/reprocessar?inicio=${inicio}&fim=${fim}`, null);
};
