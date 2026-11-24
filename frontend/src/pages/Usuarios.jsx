import React, { useState } from 'react';
import { api } from '../api.js';
import { Carregando, Erro, Ok, PERFIL, Pagina, useCarregar } from '../ui.jsx';

const NOVO = { nome: '', email: '', senha: '', perfil: 'PROFESSOR', telefone: '', matricula: '', consenteWhatsapp: false, ativo: true };

export default function Usuarios() {
  const lista = useCarregar(() => api.get('/usuarios'), []);
  const [f, setF] = useState(NOVO);
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');

  async function criar(e) {
    e.preventDefault(); setErro(''); setOk('');
    try { await api.post('/usuarios', f); setOk('Usuário criado.'); setF(NOVO); lista.recarregar(); } catch (ex) { setErro(ex.message); }
  }
  async function alternar(u) {
    setErro(''); setOk('');
    try { await api.put('/usuarios/' + u.id, { ...u, senha: null, ativo: !u.ativo }); lista.recarregar(); } catch (ex) { setErro(ex.message); }
  }
  async function mudarPerfil(u, perfil) {
    setErro(''); setOk('');
    try { await api.put('/usuarios/' + u.id, { ...u, senha: null, perfil }); setOk('Perfil atualizado.'); lista.recarregar(); } catch (ex) { setErro(ex.message); }
  }

  return (
    <Pagina titulo="Usuários e perfis" sub="Controle de acesso por papéis (RBAC). A senha precisa ter no mínimo 12 caracteres.">
      <form className="painel" onSubmit={criar}>
        <Erro msg={erro} onClose={() => setErro('')} /><Ok msg={ok} />
        <div className="form-grade">
          <label>Nome<input value={f.nome} onChange={(e) => setF({ ...f, nome: e.target.value })} required /></label>
          <label>E-mail<input type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} required /></label>
          <label>Senha inicial<input type="password" value={f.senha} onChange={(e) => setF({ ...f, senha: e.target.value })} minLength={12} required /></label>
          <label>Perfil
            <select value={f.perfil} onChange={(e) => setF({ ...f, perfil: e.target.value })}>
              {Object.entries(PERFIL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </select>
          </label>
          <label>Matrícula<input value={f.matricula} onChange={(e) => setF({ ...f, matricula: e.target.value })} /></label>
          <label>Telefone (+55…)<input value={f.telefone} onChange={(e) => setF({ ...f, telefone: e.target.value })} /></label>
        </div>
        <label className="check"><input type="checkbox" checked={f.consenteWhatsapp} onChange={(e) => setF({ ...f, consenteWhatsapp: e.target.checked })} />
          Consente em receber notificações por WhatsApp (LGPD)</label>
        <div style={{ marginTop: '1rem' }}><button className="btn">Criar usuário</button></div>
      </form>
      {lista.carregando && !lista.dados && <Carregando />}
      {lista.dados && (
        <div className="tabela"><table>
          <thead><tr><th>Nome</th><th>E-mail</th><th>Perfil</th><th>Situação</th><th /></tr></thead>
          <tbody>{lista.dados.map((u) => (
            <tr key={u.id}><td>{u.nome}<span className="nota">{u.matricula}</span></td><td>{u.email}</td>
              <td><select value={u.perfil} onChange={(e) => mudarPerfil(u, e.target.value)} aria-label={`Perfil de ${u.nome}`}>
                {Object.entries(PERFIL).map(([k, v]) => <option key={k} value={k}>{v}</option>)}</select></td>
              <td>{u.ativo ? 'Ativo' : 'Desativado'}</td>
              <td><button className="btn sec peq" onClick={() => alternar(u)}>{u.ativo ? 'Desativar' : 'Reativar'}</button></td></tr>
          ))}</tbody>
        </table></div>
      )}
    </Pagina>
  );
}
