// UC09 — Alertas → /api/alertas/*
import { apiGet } from './apiClient';

export const getAlertasAtivos = () => apiGet('/api/alertas/ativos');
export const getQuarentena    = () => apiGet('/api/alertas/quarentena');
