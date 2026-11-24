import React from 'react';
import { api } from '../api.js';
import { Carregando, Erro, Pagina, fmtInstante, useCarregar } from '../ui.jsx';

export default function Auditoria() {
  const res = useCarregar(() => api.get('/auditoria'), []);
  return (
    <Pagina titulo="Trilha de auditoria" sub="Últimas 200 operações: autor, data e hora, ação e valores antes e depois.">
      <Erro msg={res.erro} />
      {res.carregando && !res.dados && <Carregando />}
      {res.dados && (
        <div className="tabela"><table>
          <thead><tr><th>Quando</th><th>Autor</th><th>Objeto</th><th>Ação</th><th>Antes</th><th>Depois</th></tr></thead>
          <tbody>{res.dados.map((a) => (
            <tr key={a.id}><td>{fmtInstante(a.ocorridoEm)}</td><td>{a.autor}</td><td>{a.entidade}{a.entidadeId ? ` #${a.entidadeId}` : ''}</td>
              <td>{a.acao}</td><td className="meta">{a.dadosAnteriores}</td><td className="meta">{a.dadosPosteriores}</td></tr>
          ))}</tbody>
        </table></div>
      )}
    </Pagina>
  );
}
