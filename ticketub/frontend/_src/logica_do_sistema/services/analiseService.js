// UC05 — Procura em Tempo Real → /api/procura/*
// UC10 — Histórico             → /api/historico/*
// UC04 — Dashboard KPIs        → /api/dashboard/*

const BASE_PROCURA   = 'http://localhost:8080/api/procura';
const BASE_HISTORICO = 'http://localhost:8080/api/historico';
const BASE_DASHBOARD = 'http://localhost:8080/api/dashboard';

const fetchJSON = async (url) => {
  const res = await fetch(url, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    cache: 'no-store'
  });
  if (!res.ok) throw new Error(`Erro ${res.status} em ${url}`);
  return res.json();
};

export const getTempoReal          = () => fetchJSON(`${BASE_PROCURA}/tempo-real`);
export const getPorHorario         = () => fetchJSON(`${BASE_PROCURA}/por-horario`);
export const getPorLinha           = () => fetchJSON(`${BASE_PROCURA}/por-linha`);
export const getPorParagem         = () => fetchJSON(`${BASE_PROCURA}/por-paragem`);
export const getTop10Linhas        = () => fetchJSON(`${BASE_PROCURA}/por-linha/top-10`);
export const getDetalheHorarioLinha = (routeId) => fetchJSON(`${BASE_PROCURA}/por-linha/${routeId}/horario`);
export const getMetricasIngestao   = () => fetchJSON(`${BASE_DASHBOARD}/metricas-ingestao`);
export const getComparacaoPeriodos = (inicio, fim) =>
  fetchJSON(`${BASE_HISTORICO}/comparacao-periodos?inicio=${inicio}&fim=${fim}`);
