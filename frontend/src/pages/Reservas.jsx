import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Badge, Carregando, Erro, Ok, Pagina, STATUS, Vazio, fmtData, hm, useCarregar } from '../ui.jsx';

const ATIVAS = ['CONFIRMADA', 'PENDENTE_ANALISE', 'PENDENTE_TI'];

export default function Reservas() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [status, setStatus] = useState(user.perfil === 'ANALISTA_TI' ? 'PENDENTE_TI' : '');
  const res = useCarregar(() => api.get('/reservas' + (status ? `?status=${status}` : '')), [status]);
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');

  async function acao(fn, msg) {
    setErro(''); setOk('');
    try { await fn(); setOk(msg); res.recarregar(); } catch (e) { setErro(`${e.codigo && e.codigo !== 'E00' ? e.codigo + ' — ' : ''}${e.message}`); }
  }
  const pedir = (txt) => window.prompt(txt);

  function botoes(r) {
    const p = user.perfil, b = [];
    const ativa = ATIVAS.includes(r.status);
    if (ativa && ['PROFESSOR', 'TECNICO', 'COORDENADOR'].includes(p)) {
      b.push(<button key="e" className="btn sec peq" onClick={() => nav(`/reservas/${r.id}/editar`)}>Alterar</button>);
      b.push(<button key="c" className="btn sec peq" onClick={() => { const m = pedir('Motivo do cancelamento:'); if (m) acao(() => api.post(`/reservas/${r.id}/cancelar`, { motivo: m }), 'Reserva cancelada.'); }}>Cancelar</button>);
    }
    if (r.status === 'PENDENTE_ANALISE' && (p === 'TECNICO' || p === 'COORDENADOR')) {
      const bloqueado = r.urgente && p === 'TECNICO';
      if (r.softwareTerceiros)
        b.push(<button key="t" className="btn peq" disabled={bloqueado} onClick={() => acao(() => api.post(`/reservas/${r.id}/encaminhar-ti`), 'Encaminhada à TI.')}>Encaminhar à TI</button>);
      else
        b.push(<button key="a" className="btn peq" disabled={bloqueado} title={bloqueado ? 'Urgências são decididas pelo coordenador' : ''} onClick={() => acao(() => api.post(`/reservas/${r.id}/confirmar`), 'Reserva confirmada.')}>Confirmar</button>);
      b.push(<button key="r" className="btn perigo peq" disabled={bloqueado} onClick={() => { const m = pedir('Motivo da recusa:'); if (m) acao(() => api.post(`/reservas/${r.id}/recusar`, { motivo: m }), 'Reserva recusada.'); }}>Recusar</button>);
    }
    if (r.status === 'PENDENTE_TI' && p === 'ANALISTA_TI') {
      b.push(<button key="ta" className="btn peq" onClick={() => acao(() => api.post(`/reservas/${r.id}/validar-ti`, { aprovado: true, parecer: 'Software validado.' }), 'Software aprovado.')}>Aprovar software</button>);
      b.push(<button key="tr" className="btn perigo peq" onClick={() => { const m = pedir('Parecer (motivo da reprovação):'); if (m) acao(() => api.post(`/reservas/${r.id}/validar-ti`, { aprovado: false, parecer: m }), 'Software reprovado.'); }}>Reprovar</button>);
    }
    return b;
  }

  return (
    <Pagina titulo={user.perfil === 'PROFESSOR' ? 'Minhas reservas' : 'Reservas'}
            sub={user.perfil === 'ANALISTA_TI' ? 'Reservas que exigem software de terceiros.' : 'Acompanhe o ciclo de vida de cada solicitação.'}
            acoes={['PROFESSOR', 'TECNICO'].includes(user.perfil) && <button className="btn" onClick={() => nav('/reservas/nova')}>Nova reserva</button>}>
      <div className="painel filtros">
        <label>Situação
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">Todas</option>
            {Object.keys(STATUS).filter((k) => ['PENDENTE_ANALISE', 'PENDENTE_TI', 'CONFIRMADA', 'RECUSADA', 'CANCELADA', 'EXPIRADA', 'CONCLUIDA'].includes(k))
              .map((k) => <option key={k} value={k}>{STATUS[k][0]}</option>)}
          </select>
        </label>
      </div>
      <Erro msg={erro} onClose={() => setErro('')} /><Ok msg={ok} /><Erro msg={res.erro} />
      {res.carregando && !res.dados && <Carregando />}
      {res.dados && res.dados.length === 0 && <Vazio>Nenhuma reserva encontrada.</Vazio>}
      {res.dados && res.dados.length > 0 && (
        <div className="tabela">
          <table>
            <thead><tr><th>#</th><th>Laboratório</th><th>Turma</th><th>Data e horário</th><th>Situação</th><th>Ações</th></tr></thead>
            <tbody>
              {res.dados.map((r) => (
                <tr key={r.id}>
                  <td>{r.id}</td>
                  <td>{r.laboratorio}{r.recursos.length > 0 && <span className="nota">{r.recursos.map((i) => `${i.nome} ×${i.quantidade}`).join(', ')}</span>}</td>
                  <td>{r.turma} — {r.disciplina}<span className="nota">{r.professor} · {r.numeroAlunos} alunos · origem {r.origem.toLowerCase()}</span></td>
                  <td>{fmtData(r.data)}<span className="nota">{hm(r.horaInicio)}–{hm(r.horaFim)}</span></td>
                  <td><Badge status={r.status} />
                    {r.urgente && <span className="nota">Urgência: {r.justificativaUrgencia}</span>}
                    {r.softwareTerceiros && <span className="nota">Software de terceiros</span>}
                    {r.motivoDecisao && <span className="nota">{r.motivoDecisao}</span>}
                    {r.cancelamentoTardio && <span className="nota">Cancelamento tardio</span>}</td>
                  <td><div className="acoes-linha">{botoes(r)}</div></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Pagina>
  );
}
