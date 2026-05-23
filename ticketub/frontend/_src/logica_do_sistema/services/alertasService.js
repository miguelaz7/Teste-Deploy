// UC09 — Alertas → /api/alertas/*

const BASE = 'http://localhost:8080/api/alertas';

const fetchJSON = async (path) => {
  const res = await fetch(`${BASE}${path}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    cache: 'no-store'
  });
  if (!res.ok) throw new Error(`Erro ${res.status} em ${BASE}${path}`);
  return res.json();
};

export const getAlertasAtivos = () => fetchJSON('/ativos');
export const getQuarentena    = () => fetchJSON('/quarentena');
