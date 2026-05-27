// UC01 – Cliente HTTP autenticado
// Injeta o token Auth0 no header Authorization de todos os pedidos à API.
// Usado por todos os services para garantir que o backend (auth-enabled=true)
// recebe o JWT válido em cada chamada.

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080';
const AUTH_ENABLED = process.env.REACT_APP_AUTH_ENABLED !== 'false';

// Referência global ao getter do token (injetada pelo useAuthFlow via setTokenGetter)
let _getToken = null;

export function setTokenGetter(fn) {
  _getToken = fn;
}

async function getAuthHeaders() {
  if (!AUTH_ENABLED || !_getToken) return { 'Content-Type': 'application/json' };
  try {
    const token = await _getToken();
    return {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    };
  } catch {
    return { 'Content-Type': 'application/json' };
  }
}

export async function apiGet(path) {
  const headers = await getAuthHeaders();
  const res = await fetch(`${API_BASE}${path}`, { method: 'GET', headers, cache: 'no-store' });
  if (!res.ok) throw new Error(`Erro ${res.status} em ${path}`);
  return res.json();
}

export async function apiPost(path, body) {
  const headers = await getAuthHeaders();
  const res = await fetch(`${API_BASE}${path}`, { method: 'POST', headers, body: JSON.stringify(body) });
  if (!res.ok) throw new Error(`Erro ${res.status} em ${path}`);
  return res.json();
}

export async function apiPut(path, body) {
  const headers = await getAuthHeaders();
  const res = await fetch(`${API_BASE}${path}`, { method: 'PUT', headers, body: JSON.stringify(body) });
  if (!res.ok) throw new Error(`Erro ${res.status} em ${path}`);
  return res.json();
}

export async function apiDelete(path) {
  const headers = await getAuthHeaders();
  const res = await fetch(`${API_BASE}${path}`, { method: 'DELETE', headers });
  if (!res.ok) throw new Error(`Erro ${res.status} em ${path}`);
  if (res.status === 204) return true;
  return res.json();
}
