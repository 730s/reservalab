import React from 'react';
import { api } from '../api.js';
import { Badge, Carregando, Erro, Pagina, Vazio, fmtData, hm, useCarregar } from '../ui.jsx';

export default function Ensalamento() {
  const res = useCarregar(() => api.get('/ensalamento'), []);
  return (
    <Pagina titulo="Ensalamento" sub="Onde e quando acontecem as aulas práticas das suas turmas.">
      <Erro msg={res.erro} />
      {res.carregando && !res.dados && <Carregando />}
      {res.dados && res.dados.length === 0 && <Vazio>Nenhuma aula prática agendada.</Vazio>}
      {(res.dados || []).map((r) => (
        <article className="lab-card" key={r.id}>
          <div className="lab-topo">
            <div><h2>{r.disciplina}</h2><span className="meta">Turma {r.turma} · {r.professor}</span></div>
            <Badge status={r.status} />
          </div>
          <p style={{ margin: '.6rem 0 0' }}><b>{r.laboratorio}</b> · {fmtData(r.data)} · {hm(r.horaInicio)}–{hm(r.horaFim)}</p>
        </article>
      ))}
    </Pagina>
  );
}
