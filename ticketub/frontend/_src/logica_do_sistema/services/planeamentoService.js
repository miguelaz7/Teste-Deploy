// UC11 — Planeamento/ERP → /api/planeamento/*, /api/simulacao/*, /api/erp/*

const BASE_PLANEAMENTO = 'http://localhost:8080/api/planeamento';
const BASE_SIMULACAO   = 'http://localhost:8080/api/simulacao';
const BASE_ERP         = 'http://localhost:8080/api/erp';

const fetchJSON = async (url, method = 'GET', body = null) => {
  const opts = { method, headers: { 'Content-Type': 'application/json' }, cache: 'no-store' };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(url, opts);
  if (!res.ok) throw new Error(`Erro ${res.status} em ${url}`);
  return res.json();
};

export const getCenarios         = () => fetchJSON(`${BASE_PLANEAMENTO}/cenarios`);
export const submeterSimulacao   = (payload) => fetchJSON(`${BASE_PLANEAMENTO}/simular`, 'POST', payload);
export const getProjecao         = () => fetchJSON(`${BASE_SIMULACAO}/projecao`);
export const getDadosFinanceiros = () => fetchJSON(`${BASE_ERP}/dados-financeiros`);
export const gerarParaERP        = (payload) => fetchJSON(`${BASE_ERP}/dados-financeiros`, 'POST', payload);
