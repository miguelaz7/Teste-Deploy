const API_BASE_URL = "http://localhost:8080/api/ticketing";

export async function importTicketingFixedFile() {
  const response = await fetch(`${API_BASE_URL}/import`, {
    method: "POST",
  });

  const data = await response.json();
  return { ok: response.ok, data };
}

export async function fetchRevenueByRoute() {
  const response = await fetch(`${API_BASE_URL}/analytics/revenue-by-route`);
  const data = await response.json();
  return { ok: response.ok, data };
}

export async function fetchTripsByDay() {
  const response = await fetch(`${API_BASE_URL}/analytics/trips-by-day`);
  const data = await response.json();
  return { ok: response.ok, data };
}

export async function fetchTicketTypes() {
  const response = await fetch(`${API_BASE_URL}/analytics/ticket-types`);
  const data = await response.json();
  return { ok: response.ok, data };
}

export async function fetchAnomalies() {
  const response = await fetch(`${API_BASE_URL}/analytics/anomalies`);
  const data = await response.json();
  return { ok: response.ok, data };
}
