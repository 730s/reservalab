import React, { createContext, useContext, useEffect, useState } from 'react';
import { api, getSession, setSession } from './api.js';

const Ctx = createContext(null);
export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getSession()?.usuario || null);

  useEffect(() => {
    const f = () => setUser(null);
    window.addEventListener('rl:logout', f);
    return () => window.removeEventListener('rl:logout', f);
  }, []);

  // Sessão de 30 min de inatividade (RNF14): renova o token a cada 10 min enquanto houver atividade.
  useEffect(() => {
    if (!user) return undefined;
    let ativo = false;
    const marcar = () => { ativo = true; };
    const eventos = ['click', 'keydown', 'mousemove', 'touchstart'];
    eventos.forEach((e) => window.addEventListener(e, marcar));
    const t = setInterval(async () => {
      if (!ativo) return;
      ativo = false;
      try { setSession(await api.post('/auth/renovar')); } catch { /* o 401 já encerra a sessão */ }
    }, 10 * 60 * 1000);
    return () => { eventos.forEach((e) => window.removeEventListener(e, marcar)); clearInterval(t); };
  }, [user]);

  async function login(email, senha) {
    const r = await api.post('/auth/login', { email, senha });
    setSession(r);
    setUser(r.usuario);
  }
  function logout() { setSession(null); setUser(null); }

  return <Ctx.Provider value={{ user, login, logout }}>{children}</Ctx.Provider>;
}
