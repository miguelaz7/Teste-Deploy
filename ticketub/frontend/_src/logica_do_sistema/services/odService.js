// UC08 — Origem-Destino → /api/od/*
// UC12 — Exportação     → /api/exportacao/*

const BASE_OD  = 'http://localhost:8080/api/od';
const BASE_EXP = 'http://localhost:8080/api/exportacao';

const fetchJSON = async (url) => {
  const res = await fetch(url, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
    cache: 'no-store'
  });
  if (!res.ok) throw new Error(`Erro ${res.status} em ${url}`);
  return res.json();
};

export const getFluxos       = () => fetchJSON(`${BASE_OD}/fluxos`);
export const getDadosAbertos = () => fetchJSON(`${BASE_EXP}/dados-abertos`);

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
