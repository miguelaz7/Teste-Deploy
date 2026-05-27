// UC09 — Alertas → /api/alertas/*
import { apiGet, apiPost } from './apiClient';

export const getAlertasAtivos    = () => apiGet('/api/alertas/ativos');
export const getQuarentena       = () => apiGet('/api/alertas/quarentena');
export const getAnomalias        = () => apiGet('/api/alertas/anomalias');
export const getSumarioAnomalias = () => apiGet('/api/alertas/sumario-anomalias');
export const getMapaCalor        = () => apiGet('/api/alertas/mapa-calor');

export const assignAlerta = (id, atribuidoA) => 
  apiPost(`/api/alertas/${id}/atribuir?atribuidoA=${encodeURIComponent(atribuidoA)}`, null);

export const resolveAlerta = (id, accaoResolucao) => 
  apiPost(`/api/alertas/${id}/resolver?accaoResolucao=${encodeURIComponent(accaoResolucao)}`, null);

export const markFalsePositive = (id, accaoResolucao) => 
  apiPost(`/api/alertas/${id}/falso-positivo?accaoResolucao=${encodeURIComponent(accaoResolucao)}`, null);

export const confirmarEscalacao = (id) => 
  apiPost(`/api/alertas/${id}/confirmar-escalacao`, null);
