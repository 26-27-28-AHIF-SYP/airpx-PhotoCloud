const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || "/api").replace(/\/+$/, "");

let authorization = null;

async function request(path, options = {}) {
  const headers = new Headers(options.headers);
  if (authorization) headers.set("Authorization", authorization);

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new Error(body?.message || `Request failed (${response.status})`);
  }
  return body;
}

export async function login(email, password) {
  const result = await request("/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!result?.accessToken) throw new Error("Login response did not include an access token.");
  authorization = `${result.tokenType || "Bearer"} ${result.accessToken}`;
  return result;
}

export async function register(username, email, password) {
  return request("/auth/register", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, email, password }),
  });
}

export async function listPictures() {
  const pictures = await request("/pictures");
  if (!Array.isArray(pictures)) throw new Error("Unexpected photo list response.");
  return pictures;
}

export function clearAccessToken() {
  authorization = null;
}
