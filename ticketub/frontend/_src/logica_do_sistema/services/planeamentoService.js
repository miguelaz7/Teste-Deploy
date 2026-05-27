// UC11 — Planeamento/ERP → /api/planeamento/*, /api/simulacao/*, /api/erp/*
import { apiGet, apiPost } from './apiClient';

export const getCenarios         = () => apiGet('/api/planeamento/cenarios');
export const submeterSimulacao   = (payload) => apiPost('/api/planeamento/simular', payload);
export const getProjecao         = (routeId = "12", ajustePercent = 0, diasHistorico = 30) =>
  apiGet(`/api/simulacao/projecao?routeId=${routeId}&ajustePercent=${ajustePercent}&diasHistorico=${diasHistorico}`);
export const getDadosFinanceiros = () => apiGet('/api/erp/dados-financeiros');
export const gerarParaERP        = (payload) => apiPost('/api/planeamento/erp/gerar', payload);