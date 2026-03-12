import { getApiBaseUrl } from "./apiConfig";

const BILHETES_API_BASE_URL = `${getApiBaseUrl()}/bilhetes`;

function buildBilhetesUrl(filters = {}) {
  const params = new URLSearchParams();

  if (filters.codigo) {
    params.set("codigo", filters.codigo);
  }

  if (filters.nome) {
    params.set("nome", filters.nome);
  }

  if (filters.minPreco) {
    params.set("minPreco", filters.minPreco);
  }

  if (filters.maxPreco) {
    params.set("maxPreco", filters.maxPreco);
  }

  if (filters.soDisponiveis) {
    params.set("soDisponiveis", "true");
  }

  const queryString = params.toString();
  return queryString ? `${BILHETES_API_BASE_URL}?${queryString}` : BILHETES_API_BASE_URL;
}

export async function fetchBilhetes(filters = {}) {
  const response = await fetch(buildBilhetesUrl(filters), {
    method: "GET",
    headers: { "Content-Type": "application/json" },
  });

  if (!response.ok) {
    throw new Error("Nao foi possivel carregar os bilhetes.");
  }

  return response.json();
}

export function subscribeBilhetesUpdates({ onUpdate, onConnected, onError }) {
  const streamUrl = `${BILHETES_API_BASE_URL}/stream`;
  const eventSource = new EventSource(streamUrl);

  eventSource.addEventListener("connected", () => {
    if (onConnected) {
      onConnected();
    }
  });

  eventSource.addEventListener("bilhetes-updated", () => {
    if (onUpdate) {
      onUpdate();
    }
  });

  eventSource.onerror = () => {
    if (onError) {
      onError();
    }
  };

  return () => {
    eventSource.close();
  };
}
