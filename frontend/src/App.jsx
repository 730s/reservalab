import React from 'react';
import { Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import { PERFIL } from './ui.jsx';
import Login from './pages/Login.jsx';
import Disponibilidade from './pages/Disponibilidade.jsx';
import ReservaForm from './pages/ReservaForm.jsx';
import Reservas from './pages/Reservas.jsx';
import Laboratorios from './pages/Laboratorios.jsx';
import Bloqueios from './pages/Bloqueios.jsx';
import Usuarios from './pages/Usuarios.jsx';
import Turmas from './pages/Turmas.jsx';
import Ensalamento from './pages/Ensalamento.jsx';
import Relatorios from './pages/Relatorios.jsx';
import Auditoria from './pages/Auditoria.jsx';
import Notificacoes from './pages/Notificacoes.jsx';

const D = ['/disponibilidade', 'Disponibilidade'];
const NOTIF = ['/notificacoes', 'Notificações'];
const MENU = {
  PROFESSOR: [D, ['/reservas/nova', 'Nova reserva'], ['/reservas', 'Minhas reservas'], ['/ensalamento', 'Ensalamento'], NOTIF],
  TECNICO: [D, ['/reservas/nova', 'Reserva assistida'], ['/reservas', 'Reservas'], ['/laboratorios', 'Laboratórios'], ['/relatorios', 'Relatórios'], NOTIF],
  COORDENADOR: [D, ['/reservas', 'Reservas'], ['/laboratorios', 'Laboratórios'], ['/bloqueios', 'Bloqueios de agenda'], ['/turmas', 'Turmas e importação'], ['/relatorios', 'Relatórios'], ['/auditoria', 'Auditoria'], NOTIF],
  ANALISTA_TI: [['/reservas', 'Validações de software'], ['/usuarios', 'Usuários e perfis'], ['/auditoria', 'Auditoria'], NOTIF],
  ALUNO: [['/ensalamento', 'Meu ensalamento'], NOTIF],
  DIRECAO: [['/relatorios', 'Ocupação e ociosidade']],
};

export default function App() {
  const { user, logout } = useAuth();
  if (!user) return <Login />;
  const menu = MENU[user.perfil] || [];
  return (
    <div className="shell">
      <aside className="lateral">
        <div className="marca"><span className="marca-simbolo" aria-hidden="true">RL</span><span><strong>ReservaLab</strong><small>Laboratórios de aulas práticas</small></span></div>
        <nav aria-label="Principal">
          {menu.map(([to, nome]) => <NavLink key={to} to={to} end>{nome}</NavLink>)}
        </nav>
        <div className="usuario">
          <div><b>{user.nome}</b>{PERFIL[user.perfil]}</div>
          <button className="btn sec peq" onClick={logout}>Sair</button>
        </div>
      </aside>
      <main className="conteudo">
        <Routes>
          <Route path="/disponibilidade" element={<Disponibilidade />} />
          <Route path="/reservas/nova" element={<ReservaForm />} />
          <Route path="/reservas/:id/editar" element={<ReservaForm />} />
          <Route path="/reservas" element={<Reservas />} />
          <Route path="/laboratorios" element={<Laboratorios />} />
          <Route path="/bloqueios" element={<Bloqueios />} />
          <Route path="/usuarios" element={<Usuarios />} />
          <Route path="/turmas" element={<Turmas />} />
          <Route path="/ensalamento" element={<Ensalamento />} />
          <Route path="/relatorios" element={<Relatorios />} />
          <Route path="/auditoria" element={<Auditoria />} />
          <Route path="/notificacoes" element={<Notificacoes />} />
          <Route path="*" element={<Navigate to={menu[0]?.[0] || '/'} replace />} />
        </Routes>
      </main>
    </div>
  );
}
