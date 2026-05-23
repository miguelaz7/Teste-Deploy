// UC03.2 — Políticas RGPD → /api/rgpd/politicas

const BASE = 'http://localhost:8080/api/rgpd/politicas';

const fetchRGPD = async (path = '', method = 'GET', body = null) => {
  const opts = { method, headers: { 'Content-Type': 'application/json' }, cache: 'no-store' };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`${BASE}${path}`, opts);
  if (!res.ok) throw new Error(`Erro ${res.status} em ${BASE}${path}`);
  if (method === 'DELETE' || res.status === 204) return true;
  return res.json();
};

export const getPoliticas   = ()           => fetchRGPD();
export const createPolitica = (data)       => fetchRGPD('', 'POST', data);
export const updatePolitica = (id, data)   => fetchRGPD(`/${id}`, 'PUT', data);
export const deletePolitica = (id)         => fetchRGPD(`/${id}`, 'DELETE');
