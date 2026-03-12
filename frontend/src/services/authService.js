import { getApiBaseUrl } from "./apiConfig";

const AUTH_API_BASE_URL = `${getApiBaseUrl()}/auth`;

export async function sendAuthRequest(mode, payload) {
  const endpoint = mode === "register" ? "register" : "login";
  const response = await fetch(`${AUTH_API_BASE_URL}/${endpoint}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  const data = await response.json();
  return { ok: response.ok, data };
}
