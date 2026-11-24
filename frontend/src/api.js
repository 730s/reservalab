// Cliente HTTP da API. O token fica em sessionStorage (some ao fechar a aba: bom para PCs compartilhados).
const KEY = 'rl_session';
let session = null;
try { session = JSON.parse(sessionStorage.getItem(KEY) || 'null'); } catch { session = null; }

export const getSession = () => session;
export function setSession(s) {
  session = s;
  if (s) sessionStorage.setItem(KEY, JSON.stringify(s));
  else sessionStorage.removeItem(KEY);
}

export class ApiError extends Error {
  constructor(msg, codigo, status) { super(msg); this.codigo = codigo; this.status = status; }
}

async function req(method, path, body, isForm) {
  const headers = {};
  if (session?.token) headers.Authorization = 'Bearer ' + session.token;
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  let res;
  try {
    res = await fetch('/api' + path, {
      method, headers,
      body: body === undefined ? undefined : (isForm ? body : JSON.stringify(body)),
    });
  } catch {
    throw new ApiError('Não foi possível conectar ao servidor.', 'REDE', 0);
  }
  if (res.status === 401 && path !== '/auth/login') {
    setSession(null);
    window.dispatchEvent(new Event('rl:logout'));
    throw new ApiError('Sessão expirada. Entre novamente.', 'E00', 401);
  }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const fallback = res.status === 403 ? 'Você não tem permissão para esta ação.' : `Erro inesperado (${res.status}).`;
    throw new ApiError(data?.mensagem || fallback, data?.codigo || 'E00', res.status);
  }
  return data;
}

export const api = {
  get: (p) => req('GET', p),
  post: (p, b) => req('POST', p, b ?? {}),
  put: (p, b) => req('PUT', p, b ?? {}),
  patch: (p, b) => req('PATCH', p, b ?? {}),
  del: (p) => req('DELETE', p),
  upload: (p, formData) => req('POST', p, formData, true),
};
