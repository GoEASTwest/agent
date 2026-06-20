const apiBase = (import.meta.env.VITE_API_BASE || '/api').replace(/\/$/, '');

export function apiUrl(path) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${apiBase}${normalizedPath}`;
}

export async function fetchJson(path, options = {}) {
  const response = await fetch(apiUrl(path), {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) }
  });
  if (!response.ok) {
    const message = await response.text().catch(() => '');
    throw new Error(message || `Request failed: ${path}`);
  }
  return response.json();
}
