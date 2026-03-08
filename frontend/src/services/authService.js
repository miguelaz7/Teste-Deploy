const API_BASE_URL = "http://localhost:8080/api/auth";

export async function sendAuthRequest(mode, payload) {
  const endpoint = mode === "register" ? "register" : "login";
  const response = await fetch(`${API_BASE_URL}/${endpoint}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  return { ok: response.ok, data };
}
