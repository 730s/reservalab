import React, { useEffect, useState } from 'react';

export const PERFIL = {
  PROFESSOR: 'Professor', TECNICO: 'Técnico de laboratório', COORDENADOR: 'Coordenador',
  ANALISTA_TI: 'Analista de TI', ALUNO: 'Aluno', DIRECAO: 'Direção',
};

export const STATUS = {
  PENDENTE_ANALISE: ['Em análise', 'amber'], PENDENTE_TI: ['Aguardando TI', 'amber'],
  CONFIRMADA: ['Confirmada', 'green'], RECUSADA: ['Recusada', 'red'], CANCELADA: ['Cancelada', 'gray'],
  EXPIRADA: ['Expirada', 'gray'], CONCLUIDA: ['Concluída', 'blue'],
  ATIVO: ['Ativo', 'green'], MANUTENCAO: ['Em manutenção', 'amber'], INATIVO: ['Inativo', 'gray'],
  ENVIADA: ['Enviada', 'green'], PENDENTE: ['Na fila', 'amber'], FALHA: ['Falhou', 'red'],
};

export function Badge({ status }) {
  const [txt, cor] = STATUS[status] || [status, 'gray'];
  return <span className={'badge ' + cor}>{txt}</span>;
}

export const fmtData = (iso) => (iso ? iso.split('-').reverse().join('/') : '');
export const hm = (t) => (t ? t.slice(0, 5) : '');
export const fmtInstante = (iso) => (iso ? new Date(iso).toLocaleString('pt-BR') : '');
export const minutos = (t) => { const [h, m] = t.split(':'); return Number(h) * 60 + Number(m); };

export function proximoDiaUtil() {
  const d = new Date();
  do { d.setDate(d.getDate() + 1); } while (d.getDay() === 0 || d.getDay() === 6);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

export function Erro({ msg, onClose }) {
  if (!msg) return null;
  return (
    <div className="alert erro" role="alert">
      <span>{msg}</span>
      {onClose && <button className="link" onClick={onClose} aria-label="Fechar aviso">Fechar</button>}
    </div>
  );
}

export function Ok({ msg }) {
  return msg ? <div className="alert ok" role="status">{msg}</div> : null;
}

export function Vazio({ children }) { return <p className="vazio">{children}</p>; }

export function Carregando() { return <p className="vazio">Carregando…</p>; }

export function useCarregar(fn, deps) {
  const [s, setS] = useState({ dados: null, erro: null, carregando: true });
  const [n, setN] = useState(0);
  useEffect(() => {
    let vivo = true;
    setS((x) => ({ ...x, carregando: true, erro: null }));
    fn().then((d) => vivo && setS({ dados: d, erro: null, carregando: false }))
      .catch((e) => vivo && setS({ dados: null, erro: e.message, carregando: false }));
    return () => { vivo = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, n]);
  return { ...s, recarregar: () => setN((x) => x + 1) };
}

export function Pagina({ titulo, sub, acoes, children }) {
  return (
    <section className="pagina">
      <header className="pagina-topo">
        <div>
          <h1><span className="marcado">{titulo}</span></h1>
          {sub && <p className="sub">{sub}</p>}
        </div>
        {acoes && <div className="acoes-topo">{acoes}</div>}
      </header>
      {children}
    </section>
  );
}
