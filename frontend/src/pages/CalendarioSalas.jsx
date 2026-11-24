import React, { useState } from 'react';
import { api } from '../api.js';
import { Carregando, Erro, Vazio, minutos, useCarregar } from '../ui.jsx';

const diasDaSemana = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];
const formatoDia = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' });
const formatoCompleto = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'full' });
const estados = {
  livre: 'Livre',
  reservado: 'Reservado',
  pendente: 'Em análise',
  bloqueado: 'Bloqueado',
  manutencao: 'Em manutenção',
};

function dataLocal(data) {
  return new Date(`${data}T12:00:00`);
}

function iso(dia) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${dia.getFullYear()}-${pad(dia.getMonth() + 1)}-${pad(dia.getDate())}`;
}

export function deslocarData(data, dias) {
  const dia = dataLocal(data);
  dia.setDate(dia.getDate() + dias);
  return iso(dia);
}

export function inicioDaSemana(data) {
  return deslocarData(data, -((dataLocal(data).getDay() + 6) % 7));
}

export function hojeLocal() {
  return iso(new Date());
}

function hora(minuto) {
  return `${String(Math.floor(minuto / 60)).padStart(2, '0')}:${String(minuto % 60).padStart(2, '0')}`;
}

function tipoOcupacao(ocupacao) {
  if (ocupacao.tipo === 'BLOQUEIO') return 'bloqueado';
  if (ocupacao.status === 'PENDENTE_ANALISE' || ocupacao.status === 'PENDENTE_TI') return 'pendente';
  return 'reservado';
}

function montarDia(dados, abre, fecha) {
  if (!dados) return { blocos: [], faixas: 1 };

  const { laboratorio, ocupacoes } = dados;
  const ocupados = ocupacoes.map((ocupacao) => ({
    inicio: Math.max(abre, minutos(ocupacao.horaInicio)),
    fim: Math.min(fecha, minutos(ocupacao.horaFim)),
    tipo: tipoOcupacao(ocupacao),
    descricao: ocupacao.descricao,
  })).filter(({ inicio, fim }) => inicio < fim).sort((a, b) => a.inicio - b.inicio);

  const blocos = [];
  const finais = [];
  let proximo = abre;
  const tipoLivre = laboratorio.status === 'ATIVO' ? 'livre' : 'manutencao';
  for (const bloco of ocupados) {
    if (proximo < bloco.inicio) blocos.push({ inicio: proximo, fim: bloco.inicio, tipo: tipoLivre });
    let faixa = finais.findIndex((fim) => fim <= bloco.inicio);
    if (faixa === -1) faixa = finais.length;
    finais[faixa] = bloco.fim;
    blocos.push({ ...bloco, faixa });
    proximo = Math.max(proximo, bloco.fim);
  }
  if (proximo < fecha) blocos.push({ inicio: proximo, fim: fecha, tipo: tipoLivre });
  return { blocos, faixas: Math.max(1, finais.length) };
}

export default function CalendarioSalas({ inicio, filtros, podeReservar, salaId, definirSala, reservar }) {
  const dias = diasDaSemana.map((_, indice) => deslocarData(inicio, indice));
  const { dados, erro, carregando, recarregar } = useCarregar(
    () => Promise.all(dias.map((dia) => api.get(`/disponibilidade?data=${dia}${filtros}`))),
    [inicio, filtros],
  );
  const [selecao, selecionar] = useState(null);
  const salas = [...new Map((dados || []).flat().map(({ laboratorio }) => [laboratorio.id, laboratorio])).values()];
  const sala = salas.find(({ id }) => String(id) === salaId) || salas[0];

  if (erro) return <><Erro msg={erro} /><button className="btn sec" onClick={recarregar}>Tentar novamente</button></>;
  if (!dados) return <Carregando />;
  if (!sala) return <Vazio>Nenhum laboratório atende a esses filtros.</Vazio>;

  const abre = minutos(sala.horaAbertura);
  const fecha = minutos(sala.horaFechamento);
  const primeiraHora = Math.floor(abre / 60) * 60;
  const ultimaHora = Math.ceil(fecha / 60) * 60;
  const duracao = ultimaHora - primeiraHora;
  const horas = Array.from({ length: duracao / 60 + 1 }, (_, indice) => primeiraHora + indice * 60);
  const posicao = (minuto) => ((minuto - primeiraHora) / duracao) * 100;
  const detalhe = selecao?.salaId === sala.id ? selecao : null;
  const hoje = hojeLocal();

  return (
    <section className="agenda painel" aria-label="Calendário semanal das salas" aria-busy={carregando}>
      <header className="agenda-sala">
        <div>
          <label>Sala
            <select value={String(sala.id)} onChange={(e) => { definirSala(e.target.value); selecionar(null); }}>
              {salas.map((lab) => <option key={lab.id} value={String(lab.id)}>{lab.identificacao}</option>)}
            </select>
          </label>
          <p className="meta">{sala.localizacao} · {sala.capacidadeMaxima} lugares · {hora(abre)}–{hora(fecha)}</p>
        </div>
        <button className="btn sec" onClick={() => { selecionar(null); recarregar(); }} disabled={carregando}>
          {carregando ? 'Atualizando…' : 'Atualizar'}
        </button>
      </header>
      <ul className="agenda-legenda" aria-label="Legenda">
        {Object.entries(estados).filter(([tipo]) => tipo !== 'manutencao' || sala.status !== 'ATIVO')
          .map(([tipo, nome]) => <li className={`agenda-${tipo}`} key={tipo}><span aria-hidden="true" />{nome}</li>)}
      </ul>
      <div className="agenda-rolagem" tabIndex={0} aria-label="Grade de dias e horários">
        <div className="agenda-grade" style={{ '--agenda-altura': `${duracao / 60 * 52}px` }}>
          <div className="agenda-canto">Hora</div>
          {dias.map((dia, indice) => (
            <div className={`agenda-dia${dia === hoje ? ' hoje' : ''}`} key={dia} aria-current={dia === hoje ? 'date' : undefined}>
              <span>{diasDaSemana[indice]}</span>
              <strong>{formatoDia.format(dataLocal(dia))}</strong>
            </div>
          ))}
          <div className="agenda-horas" aria-hidden="true">
            {horas.map((minuto) => <span key={minuto} style={{ top: `${posicao(minuto)}%` }}>{hora(minuto)}</span>)}
          </div>
          {dias.map((dia, indice) => {
            const registro = dados[indice].find(({ laboratorio }) => laboratorio.id === sala.id);
            const { blocos, faixas } = montarDia(registro, abre, fecha);
            return (
              <div className="agenda-coluna" key={dia} role="group" aria-label={formatoCompleto.format(dataLocal(dia))}>
                {horas.slice(1, -1).map((minuto) => <span className="agenda-linha" key={minuto} style={{ top: `${posicao(minuto)}%` }} />)}
                {blocos.map((bloco, i) => {
                  const horario = `${hora(bloco.inicio)}–${hora(bloco.fim)}`;
                  const titulo = bloco.descricao || estados[bloco.tipo];
                  const abreReserva = bloco.tipo === 'livre' && podeReservar && dia >= hoje;
                  const largura = bloco.faixa === undefined ? 100 : 100 / faixas;
                  return (
                    <button type="button" key={i} className={`agenda-bloco agenda-${bloco.tipo}`}
                      style={{
                        top: `${posicao(bloco.inicio)}%`,
                        height: `${posicao(bloco.fim) - posicao(bloco.inicio)}%`,
                        left: `${(bloco.faixa || 0) * largura}%`,
                        width: `${largura}%`,
                      }}
                      aria-label={`${abreReserva ? 'Reservar · ' : ''}${estados[bloco.tipo]} · ${horario} · ${formatoCompleto.format(dataLocal(dia))}${bloco.descricao ? ` · ${bloco.descricao}` : ''}`}
                      aria-pressed={abreReserva ? undefined : detalhe?.bloco === bloco || (detalhe?.dia === dia && detalhe?.indice === i)}
                      title={`${horario} · ${titulo}`}
                      onClick={() => abreReserva
                        ? reservar(sala.id, dia, hora(bloco.inicio), hora(Math.min(bloco.fim, bloco.inicio + 60)))
                        : selecionar({ salaId: sala.id, dia, indice: i, bloco, horario })}>
                      <span>{titulo}</span><small>{horario}</small>
                    </button>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
      <div className="agenda-detalhe meta" aria-live="polite">
        {detalhe && <><b>{estados[detalhe.bloco.tipo]}</b> · {formatoDia.format(dataLocal(detalhe.dia))} · {detalhe.horario}
          {detalhe.bloco.descricao && ` · ${detalhe.bloco.descricao}`}</>}
      </div>
    </section>
  );
}
