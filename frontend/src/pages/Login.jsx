import React, { useState } from 'react';
import { useAuth } from '../auth.jsx';
import { Erro } from '../ui.jsx';

export default function Login() {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState('');
  const [busy, setBusy] = useState(false);

  async function entrar(e) {
    e.preventDefault();
    setBusy(true); setErro('');
    try { await login(email, senha); } catch (ex) { setErro(ex.message); } finally { setBusy(false); }
  }

  return (
    <div className="login">
      <div className="login-lado">
        <span className="marca-simbolo" aria-hidden="true">RL</span>
        <h1>ReservaLab</h1>
        <p>Reserve laboratórios sem planilhas, sem choque de horário e com o técnico avisado a tempo.</p>
      </div>
      <div className="login-form">
        <form className="painel" onSubmit={entrar}>
          <h2>Entrar</h2>
          <Erro msg={erro} />
          <label>E-mail<input type="email" autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus /></label>
          <label>Senha<input type="password" autoComplete="current-password" value={senha} onChange={(e) => setSenha(e.target.value)} required /></label>
          <button className="btn" disabled={busy}>{busy ? 'Entrando…' : 'Entrar'}</button>
          <div className="dica">
            Demonstração (senha <code>ReservaLab@2026!</code>): <code>professor@</code>, <code>tecnico@</code>,{' '}
            <code>coordenador@</code>, <code>ti@</code>, <code>aluno@</code>, <code>direcao@</code> + <code>reservalab.local</code>
          </div>
        </form>
      </div>
    </div>
  );
}
