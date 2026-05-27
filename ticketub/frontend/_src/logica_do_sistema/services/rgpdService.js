// UC03.2 — Políticas RGPD e Direito ao Esquecimento

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

// Exportações que requerem aprovação do DPO
export const getExportacoes = async () => {
  const res = await fetch('http://localhost:8080/api/ngsi-ld/exportacoes');
  if (!res.ok) throw new Error('Erro ao listar exportações');
  return res.json();
};

export const decisaoExportacao = async (id, decisao) => {
  const res = await fetch(`http://localhost:8080/api/ngsi-ld/exportacoes/${id}/aprovar`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json', 'X-Api-User': 'dpo' },
    body: JSON.stringify({ decisao })
  });
  if (!res.ok) throw new Error('Erro ao processar decisão de exportação');
  return res.json();
};

// Direito ao Esquecimento
export const direitoAoEsquecimento = async (cardId) => {
  const res = await fetch('http://localhost:8080/api/rgpd/direito-ao-esquecimento', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Api-User': 'dpo' },
    body: JSON.stringify({ cardId })
  });
  if (!res.ok) {
    try {
      const errorData = await res.json();
      throw new Error(errorData.error || 'Erro ao executar direito ao esquecimento');
    } catch {
      throw new Error('Erro ao executar direito ao esquecimento');
    }
  }
  return res.json();
};
