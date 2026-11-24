import React, { useState } from 'react';
import { useAuth } from '../auth.jsx';
import { Erro } from '../ui.jsx';

// Semana ilustrativa da tela de entrada (decorativa): cada linha é um horário, cada letra um dia útil.
const DIAS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex'];
const CELULA = { '.': 'livre', r: 'reservado', p: 'pendente', b: 'bloqueado', n: 'novo' };
const SEMANA = ['r..b.', 'rr.b.', '.r.p.', '..n.p', 'b.r..', '..r.r'];

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
        <div className="vitrine" aria-hidden="true">
          <span />
          {DIAS.map((d) => <span className="dia" key={d}>{d}</span>)}
          {SEMANA.map((linha, l) => (
            <React.Fragment key={l}>
              <span className="hora">{String(8 + l * 2).padStart(2, '0')}h</span>
              {[...linha].map((c, i) => (
                <span key={i} className={'cel ' + CELULA[c]} style={{ '--i': l * 5 + i }} />
              ))}
            </React.Fragment>
          ))}
        </div>
        <ul className="vitrine-legenda" aria-hidden="true">
          <li><i style={{ background: '#5b6fe0' }} />Reservado</li>
          <li><i style={{ background: '#ffe04a' }} />Em análise</li>
          <li><i style={{ background: '#4b5d63' }} />Bloqueado</li>
          <li><i style={{ background: '#2bb3a6' }} />Sua reserva</li>
        </ul>
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
