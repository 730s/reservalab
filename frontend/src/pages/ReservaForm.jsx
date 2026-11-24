import React, { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Erro, Pagina, hm, proximoDiaUtil, useCarregar } from '../ui.jsx';

export default function ReservaForm() {
  const { id } = useParams();
  const [sp] = useSearchParams();
  const nav = useNavigate();
  const { user } = useAuth();
  const turmas = useCarregar(() => api.get('/turmas'), []);
  const labs = useCarregar(() => api.get('/laboratorios'), []);
  const [f, setF] = useState({
    turmaId: '', laboratorioId: sp.get('lab') || '', data: sp.get('data') || proximoDiaUtil(),
    horaInicio: sp.get('horaInicio') || '08:00', horaFim: sp.get('horaFim') || '10:00',
    exigePreparo: false, urgente: false, justificativaUrgencia: '', origem: 'PRESENCIAL',
  });
  const [itens, setItens] = useState({});
  const [erro, setErro] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (k, v) => setF((x) => ({ ...x, [k]: v }));

  useEffect(() => {
    if (!id) return;
    api.get('/reservas/' + id).then((r) => {
      setF({ turmaId: r.turmaId, laboratorioId: r.laboratorioId, data: r.data, horaInicio: hm(r.horaInicio), horaFim: hm(r.horaFim),
        exigePreparo: r.exigePreparo, urgente: r.urgente, justificativaUrgencia: r.justificativaUrgencia || '', origem: r.origem });
      setItens(Object.fromEntries(r.recursos.map((x) => [x.recursoId, x.quantidade])));
    }).catch((e) => setErro(e.message));
  }, [id]);

  const lab = (labs.dados || []).find((l) => String(l.id) === String(f.laboratorioId));
  const turma = (turmas.dados || []).find((t) => String(t.id) === String(f.turmaId));
  const excede = lab && turma && turma.numeroAlunos > lab.capacidadeMaxima;

  async function salvar(e) {
    e.preventDefault();
    setBusy(true); setErro('');
    const body = {
      turmaId: Number(f.turmaId), laboratorioId: Number(f.laboratorioId), data: f.data,
      horaInicio: f.horaInicio, horaFim: f.horaFim, exigePreparo: f.exigePreparo,
      urgente: f.urgente, justificativaUrgencia: f.urgente ? f.justificativaUrgencia : null,
      origem: user.perfil === 'TECNICO' ? f.origem : null,
      recursos: Object.entries(itens).filter(([, q]) => Number(q) > 0).map(([rid, q]) => ({ recursoId: Number(rid), quantidade: Number(q) })),
    };
    try {
      if (id) await api.put('/reservas/' + id, body); else await api.post('/reservas', body);
      nav('/reservas');
    } catch (ex) {
      setErro(`${ex.codigo && ex.codigo !== 'E00' ? ex.codigo + ' — ' : ''}${ex.message}`);
    } finally { setBusy(false); }
  }

  return (
    <Pagina titulo={id ? `Alterar reserva #${id}` : (user.perfil === 'TECNICO' ? 'Reserva assistida' : 'Nova reserva')}
            sub={user.perfil === 'TECNICO' ? 'Registre a reserva em nome do professor que pediu por e-mail, telefone ou presencialmente.' : 'O sistema valida capacidade, conflitos e recursos antes de gravar.'}>
      <form className="painel" onSubmit={salvar}>
        <Erro msg={erro} onClose={() => setErro('')} />
        <div className="form-grade">
          <label>Turma
            <select value={f.turmaId} onChange={(e) => set('turmaId', e.target.value)} required>
              <option value="">Selecione…</option>
              {(turmas.dados || []).map((t) => <option key={t.id} value={t.id}>{t.codigo} — {t.disciplinaNome} ({t.numeroAlunos} alunos){user.perfil === 'TECNICO' ? ` · ${t.professorNome}` : ''}</option>)}
            </select>
          </label>
          <label>Laboratório
            <select value={f.laboratorioId} onChange={(e) => { set('laboratorioId', e.target.value); setItens({}); }} required>
              <option value="">Selecione…</option>
              {(labs.dados || []).filter((l) => l.status !== 'INATIVO').map((l) => <option key={l.id} value={l.id}>{l.identificacao} ({l.capacidadeMaxima} lugares)</option>)}
            </select>
          </label>
          <label>Data<input type="date" value={f.data} onChange={(e) => set('data', e.target.value)} required /></label>
          <label>Início<input type="time" value={f.horaInicio} onChange={(e) => set('horaInicio', e.target.value)} required /></label>
          <label>Fim<input type="time" value={f.horaFim} onChange={(e) => set('horaFim', e.target.value)} required /></label>
          {user.perfil === 'TECNICO' && (
            <label>Origem do pedido
              <select value={f.origem} onChange={(e) => set('origem', e.target.value)}>
                <option value="PRESENCIAL">Presencial</option><option value="EMAIL">E-mail</option><option value="TELEFONE">Telefone</option>
              </select>
            </label>
          )}
        </div>
        {excede && <Erro msg={`A turma tem ${turma.numeroAlunos} alunos e ${lab.identificacao} comporta ${lab.capacidadeMaxima}. Escolha outro laboratório.`} />}
        {lab && lab.recursos.length > 0 && (
          <>
            <h2>Recursos necessários</h2>
            <div className="form-grade">
              {lab.recursos.map((r) => (
                <label key={r.recursoId}>{r.nome}{r.softwareTerceiros ? ' (software de terceiros — exige validação da TI)' : ''} · disponível: {r.quantidade}
                  <input type="number" min="0" max={r.quantidade} value={itens[r.recursoId] ?? ''} placeholder="0"
                         onChange={(e) => setItens((x) => ({ ...x, [r.recursoId]: e.target.value }))} />
                </label>
              ))}
            </div>
          </>
        )}
        <label className="check"><input type="checkbox" checked={f.exigePreparo} onChange={(e) => set('exigePreparo', e.target.checked)} />
          A aula exige preparo de materiais pelo técnico (reagentes, equipamentos específicos)</label>
        <label className="check"><input type="checkbox" checked={f.urgente} onChange={(e) => set('urgente', e.target.checked)} />
          Reserva de urgência (abaixo da antecedência mínima; precisa da aprovação do coordenador)</label>
        {f.urgente && (
          <label>Justificativa da urgência (mínimo de 20 caracteres)
            <textarea value={f.justificativaUrgencia} onChange={(e) => set('justificativaUrgencia', e.target.value)} />
          </label>
        )}
        <div style={{ marginTop: '1rem', display: 'flex', gap: '.5rem' }}>
          <button className="btn" disabled={busy || excede}>{busy ? 'Salvando…' : (id ? 'Salvar alteração' : 'Reservar')}</button>
          <button type="button" className="btn sec" onClick={() => nav(-1)}>Voltar</button>
        </div>
      </form>
    </Pagina>
  );
}
