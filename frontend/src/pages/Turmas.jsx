import React, { useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Carregando, Erro, Ok, Pagina, useCarregar } from '../ui.jsx';

const MODELO = 'codigoDisciplina,nomeDisciplina,cargaHoraria,codigoTurma,periodoLetivo,emailProfessor,numeroAlunos,emailsAlunos\n'
  + 'FIS1,Física Experimental,60,FIS1-A,2026/2,professor@reservalab.local,28,aluno@reservalab.local|aluno2@reservalab.local\n';

export default function Turmas() {
  const { user } = useAuth();
  const turmas = useCarregar(() => api.get('/turmas'), []);
  const [rel, setRel] = useState(null);
  const [erro, setErro] = useState('');
  const [ok, setOk] = useState('');

  async function enviar(e) {
    e.preventDefault(); setErro(''); setOk(''); setRel(null);
    const arq = e.target.arquivo.files[0];
    if (!arq) return;
    const fd = new FormData(); fd.append('arquivo', arq);
    try { const r = await api.upload('/turmas/importar', fd); setRel(r); setOk('Importação concluída.'); turmas.recarregar(); }
    catch (ex) { setErro(ex.message); }
  }
  function baixarModelo() {
    const url = URL.createObjectURL(new Blob([MODELO], { type: 'text/csv;charset=utf-8' }));
    const a = document.createElement('a'); a.href = url; a.download = 'modelo-turmas.csv'; a.click(); URL.revokeObjectURL(url);
  }

  return (
    <Pagina titulo="Turmas e importação" sub="Importe turmas, disciplinas e matrículas por arquivo CSV (UTF-8).">
      {user.perfil === 'COORDENADOR' && (
        <form className="painel" onSubmit={enviar}>
          <Erro msg={erro} onClose={() => setErro('')} /><Ok msg={ok} />
          <div className="filtros">
            <label>Arquivo CSV<input type="file" name="arquivo" accept=".csv,text/csv" required /></label>
            <button className="btn">Importar</button>
            <button type="button" className="btn sec" onClick={baixarModelo}>Baixar modelo</button>
          </div>
          {rel && (
            <div style={{ marginTop: '1rem' }}>
              <p>{rel.linhasLidas} linha(s) lida(s) · {rel.turmasProcessadas} turma(s) processada(s) · {rel.matriculasCriadas} matrícula(s) criada(s).</p>
              {rel.erros.length > 0 && <Erro msg={'Inconsistências: ' + rel.erros.join(' | ')} />}
            </div>
          )}
        </form>
      )}
      {turmas.carregando && !turmas.dados && <Carregando />}
      {turmas.dados && (
        <div className="tabela"><table>
          <thead><tr><th>Turma</th><th>Disciplina</th><th>Período</th><th>Professor</th><th>Alunos</th></tr></thead>
          <tbody>{turmas.dados.map((t) => (
            <tr key={t.id}><td>{t.codigo}</td><td>{t.disciplinaNome}</td><td>{t.periodoLetivo}</td><td>{t.professorNome}</td><td>{t.numeroAlunos}</td></tr>
          ))}</tbody>
        </table></div>
      )}
    </Pagina>
  );
}
