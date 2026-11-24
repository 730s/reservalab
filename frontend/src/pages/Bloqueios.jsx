import React, { useState } from 'react';
import { api } from '../api.js';
import { Carregando, Erro, Ok, Pagina, Vazio, fmtData, hm, proximoDiaUtil, useCarregar } from '../ui.jsx';

export default function Bloqueios() {
  const labs = useCarregar(() => api.get('/laboratorios'), []);
  const lista = useCarregar(() => api.get('/bloqueios'), []);
  const [f, setF] = useState({ laboratorioId: '', data: proximoDiaUtil(), horaInicio: '08:00', horaFim: '12:00', motivo: '', cancelarConflitantes: false });
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');

  async function criar(e) {
    e.preventDefault(); setErro(''); setOk('');
    try { await api.post('/bloqueios', { ...f, laboratorioId: Number(f.laboratorioId) }); setOk('Agenda bloqueada.'); lista.recarregar(); }
    catch (ex) { setErro(ex.message); }
  }
  async function remover(id) {
    setErro('');
    try { await api.del('/bloqueios/' + id); lista.recarregar(); } catch (ex) { setErro(ex.message); }
  }

  return (
    <Pagina titulo="Bloqueios de agenda" sub="Indisponibilize um intervalo (manutenção, evento). Privativo da coordenação.">
      <form className="painel" onSubmit={criar}>
        <Erro msg={erro} onClose={() => setErro('')} /><Ok msg={ok} />
        <div className="form-grade">
          <label>Laboratório
            <select value={f.laboratorioId} onChange={(e) => setF({ ...f, laboratorioId: e.target.value })} required>
              <option value="">Selecione…</option>
              {(labs.dados || []).filter((l) => l.status !== 'INATIVO').map((l) => <option key={l.id} value={l.id}>{l.identificacao}</option>)}
            </select>
          </label>
          <label>Data<input type="date" value={f.data} onChange={(e) => setF({ ...f, data: e.target.value })} required /></label>
          <label>Início<input type="time" value={f.horaInicio} onChange={(e) => setF({ ...f, horaInicio: e.target.value })} required /></label>
          <label>Fim<input type="time" value={f.horaFim} onChange={(e) => setF({ ...f, horaFim: e.target.value })} required /></label>
          <label>Motivo<input value={f.motivo} onChange={(e) => setF({ ...f, motivo: e.target.value })} required /></label>
        </div>
        <label className="check"><input type="checkbox" checked={f.cancelarConflitantes} onChange={(e) => setF({ ...f, cancelarConflitantes: e.target.checked })} />
          Se houver reservas no intervalo, cancelá-las com este motivo e avisar os envolvidos</label>
        <div style={{ marginTop: '1rem' }}><button className="btn">Bloquear agenda</button></div>
      </form>
      {lista.carregando && !lista.dados && <Carregando />}
      {lista.dados && lista.dados.length === 0 && <Vazio>Nenhum bloqueio futuro.</Vazio>}
      {lista.dados && lista.dados.length > 0 && (
        <div className="tabela"><table>
          <thead><tr><th>Laboratório</th><th>Data</th><th>Horário</th><th>Motivo</th><th>Criado por</th><th /></tr></thead>
          <tbody>{lista.dados.map((b) => (
            <tr key={b.id}><td>{b.laboratorio}</td><td>{fmtData(b.data)}</td><td>{hm(b.horaInicio)}–{hm(b.horaFim)}</td><td>{b.motivo}</td><td>{b.criadoPor}</td>
              <td><button className="btn sec peq" onClick={() => remover(b.id)}>Desbloquear</button></td></tr>
          ))}</tbody>
        </table></div>
      )}
    </Pagina>
  );
}
