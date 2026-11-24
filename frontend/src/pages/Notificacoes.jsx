import React from 'react';
import { api } from '../api.js';
import { Badge, Carregando, Erro, Pagina, Vazio, fmtInstante, useCarregar } from '../ui.jsx';

export default function Notificacoes() {
  const res = useCarregar(() => api.get('/notificacoes'), []);
  return (
    <Pagina titulo="Notificações" sub="Avisos enviados a você por e-mail (e WhatsApp, se consentido)."
            acoes={<button className="btn sec" onClick={res.recarregar}>Atualizar</button>}>
      <Erro msg={res.erro} />
      {res.carregando && !res.dados && <Carregando />}
      {res.dados && res.dados.length === 0 && <Vazio>Você ainda não recebeu notificações.</Vazio>}
      {(res.dados || []).map((n) => (
        <article className="lab-card" key={n.id}>
          <div className="lab-topo"><h2 style={{ fontSize: '1rem' }}>{n.assunto}</h2><span className="meta">{n.canal === 'EMAIL' ? 'E-mail' : 'WhatsApp'} · <Badge status={n.status} /> · {fmtInstante(n.criadaEm)}</span></div>
          <p style={{ margin: '.5rem 0 0' }}>{n.mensagem}</p>
        </article>
      ))}
    </Pagina>
  );
}
