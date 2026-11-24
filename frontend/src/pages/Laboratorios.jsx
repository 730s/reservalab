import React, { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Badge, Carregando, Erro, Ok, Pagina, Vazio, hm, useCarregar } from '../ui.jsx';

const VAZIO = { identificacao: '', localizacao: '', capacidadeMaxima: 30, horaAbertura: '08:00', horaFechamento: '18:00',
  status: 'ATIVO', exigeAcompanhamentoTecnico: false, tecnicoIds: [] };

function RecursosEditor({ value, onChange, catalogo }) {
  const livres = catalogo.filter((c) => !value.some((v) => v.recursoId === c.id));
  return (
    <div>
      {value.map((v, i) => (
        <div className="linha-rec" key={v.recursoId}>
          <span>{catalogo.find((c) => c.id === v.recursoId)?.nome || v.recursoId}</span>
          <input type="number" min="0" value={v.quantidade} aria-label="Quantidade"
                 onChange={(e) => onChange(value.map((x, j) => (j === i ? { ...x, quantidade: Number(e.target.value) } : x)))} />
          <button type="button" className="btn sec peq" onClick={() => onChange(value.filter((_, j) => j !== i))}>Remover</button>
        </div>
      ))}
      {livres.length > 0 && (
        <select value="" onChange={(e) => e.target.value && onChange([...value, { recursoId: Number(e.target.value), quantidade: 1 }])} aria-label="Adicionar recurso">
          <option value="">+ Adicionar recurso…</option>
          {livres.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
        </select>
      )}
    </div>
  );
}

export default function Laboratorios() {
  const { user } = useAuth();
  const coord = user.perfil === 'COORDENADOR';
  const labs = useCarregar(() => api.get('/laboratorios'), []);
  const catalogo = useCarregar(() => api.get('/recursos'), []);
  const tecnicos = useCarregar(() => (coord ? api.get('/usuarios/tecnicos') : Promise.resolve([])), []);
  const [edit, setEdit] = useState(null);   // {id?, ...campos, recursos}
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');
  const [novoRec, setNovoRec] = useState({ nome: '', tipo: 'EQUIPAMENTO', softwareTerceiros: false, versao: '' });

  const cat = catalogo.dados || [];
  async function run(fn, msg) {
    setErro(''); setOk('');
    try { await fn(); setOk(msg); labs.recarregar(); catalogo.recarregar(); } catch (e) { setErro(e.message); }
  }
  const abrir = (l) => setEdit(l ? {
    id: l.id, identificacao: l.identificacao, localizacao: l.localizacao || '', capacidadeMaxima: l.capacidadeMaxima,
    horaAbertura: hm(l.horaAbertura), horaFechamento: hm(l.horaFechamento), status: l.status === 'INATIVO' ? 'INATIVO' : l.status,
    exigeAcompanhamentoTecnico: l.exigeAcompanhamentoTecnico, tecnicoIds: l.tecnicos.map((t) => t.id),
    recursos: l.recursos.map((r) => ({ recursoId: r.recursoId, quantidade: r.quantidade })),
  } : { ...VAZIO, recursos: [] });


  return (
    <Pagina titulo="Laboratórios" sub="Cadastro único de laboratórios e recursos."
            acoes={coord && <button className="btn" onClick={() => abrir(null)}>Novo laboratório</button>}>
      <Erro msg={erro} onClose={() => setErro('')} /><Ok msg={ok} /><Erro msg={labs.erro} />

      {edit && (
        <form className="painel" onSubmit={async (e) => { e.preventDefault(); const { id, ...body } = edit;
          setErro(''); setOk('');
          try { await (id ? api.put('/laboratorios/' + id, body) : api.post('/laboratorios', body)); setOk('Laboratório salvo.'); setEdit(null); labs.recarregar(); }
          catch (ex) { setErro(ex.message); } }}>
          <h2>{edit.id ? `Editar ${edit.identificacao}` : 'Novo laboratório'}</h2>
          <div className="form-grade">
            <label>Identificação<input value={edit.identificacao} onChange={(e) => setEdit({ ...edit, identificacao: e.target.value })} required /></label>
            <label>Localização<input value={edit.localizacao} onChange={(e) => setEdit({ ...edit, localizacao: e.target.value })} /></label>
            <label>Lotação máxima<input type="number" min="1" value={edit.capacidadeMaxima} onChange={(e) => setEdit({ ...edit, capacidadeMaxima: Number(e.target.value) })} required /></label>
            <label>Abertura<input type="time" value={edit.horaAbertura} onChange={(e) => setEdit({ ...edit, horaAbertura: e.target.value })} required /></label>
            <label>Fechamento<input type="time" value={edit.horaFechamento} onChange={(e) => setEdit({ ...edit, horaFechamento: e.target.value })} required /></label>
            <label>Situação
              <select value={edit.status} onChange={(e) => setEdit({ ...edit, status: e.target.value })}>
                <option value="ATIVO">Ativo</option><option value="MANUTENCAO">Em manutenção</option>
                {edit.status === 'INATIVO' && <option value="INATIVO">Inativo</option>}
              </select>
            </label>
          </div>
          <label className="check"><input type="checkbox" checked={edit.exigeAcompanhamentoTecnico} onChange={(e) => setEdit({ ...edit, exigeAcompanhamentoTecnico: e.target.checked })} />
            Exige acompanhamento técnico (reservas passam pela análise do técnico)</label>
          <h2 style={{ marginTop: '1rem' }}>Técnicos responsáveis</h2>
          <div className="chips" style={{ marginBottom: '1rem' }}>
            {(tecnicos.dados || []).map((t) => (
              <label className="check" key={t.id}><input type="checkbox" checked={edit.tecnicoIds.includes(t.id)}
                onChange={(e) => setEdit({ ...edit, tecnicoIds: e.target.checked ? [...edit.tecnicoIds, t.id] : edit.tecnicoIds.filter((x) => x !== t.id) })} />{t.nome}</label>
            ))}
          </div>
          <h2>Recursos</h2>
          <RecursosEditor value={edit.recursos} catalogo={cat} onChange={(v) => setEdit({ ...edit, recursos: v })} />
          <div style={{ marginTop: '1rem', display: 'flex', gap: '.5rem' }}>
            <button className="btn">Salvar</button>
            <button type="button" className="btn sec" onClick={() => setEdit(null)}>Cancelar</button>
          </div>
        </form>
      )}

      {labs.carregando && !labs.dados && <Carregando />}
      {labs.dados && labs.dados.length === 0 && <Vazio>Nenhum laboratório cadastrado.</Vazio>}
      {(labs.dados || []).map((l) => (
        <article className="lab-card" key={l.id}>
          <div className="lab-topo">
            <div>
              <h2>{l.identificacao} <Badge status={l.status} /></h2>
              <span className="meta">{l.localizacao} · {l.capacidadeMaxima} lugares · {hm(l.horaAbertura)}–{hm(l.horaFechamento)}
                {l.exigeAcompanhamentoTecnico && ' · acompanhamento técnico obrigatório'} · técnicos: {l.tecnicos.map((t) => t.nome).join(', ') || 'não definidos'}</span>
            </div>
            <div className="acoes-linha">
              {coord && <button className="btn sec peq" onClick={() => abrir(l)}>Editar</button>}
              {l.status !== 'INATIVO' && (
                <button className="btn sec peq" onClick={() => run(() => api.patch(`/laboratorios/${l.id}/status`, { status: l.status === 'ATIVO' ? 'MANUTENCAO' : 'ATIVO' }),
                  'Situação atualizada.')}>{l.status === 'ATIVO' ? 'Colocar em manutenção' : 'Reativar'}</button>
              )}
              {coord && l.status !== 'INATIVO' && (
                <button className="btn perigo peq" onClick={() => window.confirm(`Inativar ${l.identificacao}?`) && run(() => api.del('/laboratorios/' + l.id), 'Laboratório inativado.')}>Inativar</button>
              )}
            </div>
          </div>
          <div className="chips">{l.recursos.map((r) => <span className="chip" key={r.recursoId}>{r.nome} × {r.quantidade}{r.softwareTerceiros ? ' (terceiros)' : ''}</span>)}</div>
        </article>
      ))}

      <div className="painel">
        <h2>Catálogo de recursos e softwares</h2>
        <div className="chips">{cat.map((c) => <span className="chip" key={c.id}>{c.nome} · {c.tipo.toLowerCase()}{c.softwareTerceiros ? ' · terceiros' : ''}</span>)}</div>
        <form className="filtros" style={{ marginTop: '1rem' }} onSubmit={(e) => { e.preventDefault();
          run(() => api.post('/recursos', novoRec), 'Recurso cadastrado.'); setNovoRec({ nome: '', tipo: 'EQUIPAMENTO', softwareTerceiros: false, versao: '' }); }}>
          <label>Nome<input value={novoRec.nome} onChange={(e) => setNovoRec({ ...novoRec, nome: e.target.value })} required /></label>
          <label>Tipo
            <select value={novoRec.tipo} onChange={(e) => setNovoRec({ ...novoRec, tipo: e.target.value })}>
              <option value="EQUIPAMENTO">Equipamento</option><option value="SOFTWARE">Software</option><option value="INSUMO">Insumo</option>
            </select>
          </label>
          <label>Versão<input value={novoRec.versao} onChange={(e) => setNovoRec({ ...novoRec, versao: e.target.value })} /></label>
          {novoRec.tipo === 'SOFTWARE' && <label className="check"><input type="checkbox" checked={novoRec.softwareTerceiros} onChange={(e) => setNovoRec({ ...novoRec, softwareTerceiros: e.target.checked })} />Software de terceiros</label>}
          <button className="btn">Cadastrar recurso</button>
        </form>
      </div>
    </Pagina>
  );
}
