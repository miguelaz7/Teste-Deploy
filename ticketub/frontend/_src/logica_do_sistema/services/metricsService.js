// UC04 — Dashboard → /api/dashboard/*
import { apiGet } from './apiClient';

export const getDashboardMetrics = () => apiGet('/api/dashboard/metricas-ingestao');
