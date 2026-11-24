import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Carregando, Erro, Pagina, Vazio, fmtData, hm, minutos, proximoDiaUtil, useCarregar } from '../ui.jsx';
import CalendarioSalas, { deslocarData, hojeLocal, inicioDaSemana } from './CalendarioSalas.jsx';

export default function Disponibilidade() {
  const { user } = useAuth();
  const nav = useNavigate();
  const [data, setData] = useState(proximoDiaUtil);
  const [semanal, definirSemanal] = useState(true);
  const [salaId, definirSala] = useState('');
  const [cap, setCap] = useState('');
  const [rec, setRec] = useState('');
  const recursos = useCarregar(() => api.get('/recursos'), []);
  const filtros = `${cap ? `&capacidadeMinima=${cap}` : ''}${rec ? `&recursoId=${rec}` : ''}`;
  const inicio = inicioDaSemana(data);
  const podeReservar = user.perfil === 'PROFESSOR' || user.perfil === 'TECNICO';

  return (
    <Pagina titulo="Disponibilidade" sub="Veja em tempo real quais laboratórios estão livres e adequados à turma.">
      <div className="agenda-controles">
        <div className="agenda-modos" role="group" aria-label="Modo de visualização">
          <button className="btn peq" aria-pressed={semanal} onClick={() => definirSemanal(true)}>Calendário</button>
          <button className="btn peq" aria-pressed={!semanal} onClick={() => definirSemanal(false)}>Lista diária</button>
        </div>
        {semanal && (
          <div className="agenda-navegacao" role="group" aria-label="Navegar entre semanas">
            <button className="btn sec peq" onClick={() => setData(hojeLocal())}>Hoje</button>
            <button className="btn sec peq" aria-label="Semana anterior" onClick={() => setData((dia) => deslocarData(dia, -7))}>‹</button>
            <button className="btn sec peq" aria-label="Próxima semana" onClick={() => setData((dia) => deslocarData(dia, 7))}>›</button>
            <strong className="agenda-periodo">{fmtData(inicio)} – {fmtData(deslocarData(inicio, 6))}</strong>
          </div>
        )}
      </div>
      <div className="painel filtros">
        <label>{semanal ? 'Data na semana' : 'Data'}
          <input type="date" value={data} onChange={(e) => { if (e.target.validity.valid && e.target.value) setData(e.target.value); }} />
        </label>
        <label>Capacidade mínima<input type="number" min="1" value={cap} onChange={(e) => setCap(e.target.value)} placeholder="Nº de alunos" /></label>
        <label>Recurso
          <select value={rec} onChange={(e) => setRec(e.target.value)}>
            <option value="">Qualquer</option>
            {(recursos.dados || []).map((r) => <option key={r.id} value={r.id}>{r.nome}</option>)}
          </select>
        </label>
      </div>
      {semanal ? (
        <CalendarioSalas key={inicio + filtros} inicio={inicio} filtros={filtros} podeReservar={podeReservar}
          salaId={salaId} definirSala={definirSala}
          reservar={(lab, dia, horaInicio, horaFim) => nav(`/reservas/nova?${new URLSearchParams({ lab, data: dia, horaInicio, horaFim })}`)} />
      ) : <ListaDiaria key={data + filtros} data={data} filtros={filtros} podeReservar={podeReservar} />}
    </Pagina>
  );
}

function ListaDiaria({ data, filtros, podeReservar }) {
  const nav = useNavigate();
  const res = useCarregar(() => api.get(`/disponibilidade?data=${data}${filtros}`), [data, filtros]);
  return (
    <>
      <div className="agenda-atualizar">
        <button className="btn sec peq" onClick={res.recarregar} disabled={res.carregando}>Atualizar lista</button>
      </div>
      <Erro msg={res.erro} />
      {res.carregando && !res.dados && <Carregando />}
      {res.dados && res.dados.length === 0 && <Vazio>Nenhum laboratório atende a esses filtros.</Vazio>}
      {(res.dados || []).map(({ laboratorio: l, ocupacoes }) => {
        const ini = minutos(l.horaAbertura), fim = minutos(l.horaFechamento), total = fim - ini;
        return (
          <article className="lab-card" key={l.id}>
            <div className="lab-topo">
              <div>
                <h2>{l.identificacao}</h2>
                <span className="meta">{l.localizacao} · {l.capacidadeMaxima} lugares · {hm(l.horaAbertura)}–{hm(l.horaFechamento)}
                  {l.exigeAcompanhamentoTecnico && ' · exige acompanhamento técnico'}
                  {l.status !== 'ATIVO' && ' · em manutenção'}</span>
              </div>
              {podeReservar && l.status === 'ATIVO' && (
                <button className="btn" onClick={() => nav(`/reservas/nova?lab=${l.id}&data=${data}`)}>Reservar</button>
              )}
            </div>
            <div className="trilho" role="img" aria-label={`Ocupação de ${l.identificacao}`}>
              {ocupacoes.map((o, i) => {
                const a = Math.max(minutos(o.horaInicio), ini), b = Math.min(minutos(o.horaFim), fim);
                return (
                  <div key={i} className={'faixa ' + (o.tipo === 'BLOQUEIO' ? 'BLOQUEIO' : o.status)}
                       style={{ left: `${((a - ini) / total) * 100}%`, width: `${((b - a) / total) * 100}%` }}
                       title={`${hm(o.horaInicio)}–${hm(o.horaFim)} ${o.descricao}`}>
                    {hm(o.horaInicio)}
                  </div>
                );
              })}
            </div>
            <div className="escala"><span>{hm(l.horaAbertura)}</span><span>{hm(l.horaFechamento)}</span></div>
            {ocupacoes.length === 0
              ? <p className="legenda">{l.status === 'ATIVO' ? 'Livre o dia todo.' : 'Sala indisponível.'}</p>
              : <p className="legenda">{ocupacoes.map((o) => `${hm(o.horaInicio)}–${hm(o.horaFim)} ${o.tipo === 'BLOQUEIO' ? 'bloqueado: ' : ''}${o.descricao}`).join(' · ')}</p>}
            <div className="chips">{l.recursos.map((r) => <span className="chip" key={r.recursoId}>{r.nome} × {r.quantidade}</span>)}</div>
          </article>
        );
      })}
      <p className="legenda">Azul: reservado · âmbar: aguardando decisão (retém o horário) · cinza: bloqueio da coordenação.</p>
    </>
  );
}
