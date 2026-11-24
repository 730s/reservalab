import React, { useState } from 'react';
import { api } from '../api.js';
import { Carregando, Erro, Pagina, useCarregar } from '../ui.jsx';

const iso = (d) => d.toISOString().slice(0, 10);

export default function Relatorios() {
  const hoje = new Date();
  const [ini, setIni] = useState(iso(new Date(hoje.getFullYear(), hoje.getMonth(), 1)));
  const [fim, setFim] = useState(iso(new Date(hoje.getFullYear(), hoje.getMonth() + 1, 0)));
  const res = useCarregar(() => api.get(`/relatorios/ocupacao?inicio=${ini}&fim=${fim}`), [ini, fim]);
  const r = res.dados;
  return (
    <Pagina titulo="Ocupação e ociosidade" sub="TO = (horas disponíveis − horas ocupadas) ÷ horas disponíveis × 100. Ocupadas: reservas confirmadas ou concluídas.">
      <div className="painel filtros">
        <label>De<input type="date" value={ini} onChange={(e) => setIni(e.target.value)} /></label>
        <label>Até<input type="date" value={fim} onChange={(e) => setFim(e.target.value)} /></label>
      </div>
      <Erro msg={res.erro} />
      {res.carregando && !r && <Carregando />}
      {r && (
        <>
          <div className="kpis">
            <div className="kpi"><b>{r.taxaOciosidade}%</b><span>Taxa de ociosidade (TO)</span></div>
            <div className="kpi"><b>{r.horasOcupadas} h</b><span>Horas ocupadas de {r.horasDisponiveis} h</span></div>
            <div className="kpi"><b>{r.totalReservas}</b><span>Reservas no período</span></div>
            <div className="kpi"><b>{r.urgentes}</b><span>Urgências</span></div>
            <div className="kpi"><b>{r.cancelamentosTardios}</b><span>Cancelamentos tardios</span></div>
            <div className="kpi"><b>{r.canceladas + r.recusadas + r.expiradas}</b><span>Canceladas, recusadas e expiradas</span></div>
          </div>
          <div className="tabela"><table>
            <thead><tr><th>Laboratório</th><th>Horas disponíveis</th><th>Horas ocupadas</th><th>Ocupação</th><th>Ociosidade</th></tr></thead>
            <tbody>{r.laboratorios.map((l) => {
              const ocup = l.horasDisponiveis > 0 ? Math.min(100, (l.horasOcupadas / l.horasDisponiveis) * 100) : 0;
              return (<tr key={l.laboratorioId}><td>{l.laboratorio}</td><td>{l.horasDisponiveis}</td><td>{l.horasOcupadas}</td>
                <td><div className="barra" aria-label={`${ocup.toFixed(0)}% ocupado`}><i style={{ width: ocup + '%' }} /></div></td><td>{l.taxaOciosidade}%</td></tr>);
            })}</tbody>
          </table></div>
        </>
      )}
    </Pagina>
  );
}
