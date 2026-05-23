// UC04 — Dashboard → /api/dashboard/*

const BASE = 'http://localhost:8080/api/dashboard';

export const getDashboardMetrics = async () => {
  const res = await fetch(`${BASE}/metricas-ingestao`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    cache: 'no-store'
  });
  if (!res.ok) throw new Error(`Erro ${res.status} ao buscar métricas`);
  return res.json();
};
