package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.config.ParametrosOperacionais;
import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.*;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Serviço de aplicação (camada de controle): orquestra UC14..UC18, UC21..UC23. */
@Service
@RequiredArgsConstructor
public class ReservaService {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservaRepository reservas;
    private final LaboratorioRepository labs;
    private final TurmaRepository turmas;
    private final RecursoRepository recursos;
    private final BloqueioRepository bloqueios;
    private final ValidacaoTiRepository validacoes;
    private final UsuarioRepository usuarios;
    private final MatriculaRepository matriculas;
    private final PoliticaDeReserva politica;
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final ParametrosOperacionais params;

    // ------------------------------------------------------------------ criação (RF11-RF14, RF30)

    @Transactional
    public ReservaRes solicitar(ReservaReq req, Usuario ator) {
        Reserva r = candidata(req, ator);
        validar(r, null);
        r.setStatus(politica.decidirStatusInicial(r));
        r.setCriadaEm(Instant.now());
        r.setAtualizadaEm(r.getCriadaEm());
        persistir(r);
        auditoria.registrar(ator, "Reserva", r.getId(), "CRIAR", null, resumo(r));
        avisarCriacao(r);
        return ReservaRes.of(r);
    }

    // ------------------------------------------------------------------ alteração atômica (RF20, RN13)

    @Transactional
    public ReservaRes alterar(Long id, ReservaReq req, Usuario ator) {
        Reserva atual = buscar(id);
        exigirGestao(atual, ator);
        if (!atual.ocupaHorario()) throw ApiException.regra("Somente reservas ativas podem ser alteradas.");
        Reserva cand = candidata(req, ator);           // objeto novo: a original só muda se tudo passar
        validar(cand, atual.getId());
        StatusReserva statusAntes = atual.getStatus();
        String antes = resumo(atual);
        boolean mudouLocalOuHorario = !atual.getLaboratorio().getId().equals(cand.getLaboratorio().getId())
            || !atual.getData().equals(cand.getData())
            || !atual.getHoraInicio().equals(cand.getHoraInicio())
            || !atual.getHoraFim().equals(cand.getHoraFim());

        StatusReserva novo = politica.decidirStatusInicial(cand);
        if (novo == StatusReserva.PENDENTE_ANALISE && statusAntes == StatusReserva.PENDENTE_TI && cand.isSoftwareTerceiros())
            novo = StatusReserva.PENDENTE_TI;

        atual.setLaboratorio(cand.getLaboratorio());
        atual.setTurma(cand.getTurma());
        atual.setSolicitante(cand.getSolicitante());
        atual.setData(cand.getData());
        atual.setHoraInicio(cand.getHoraInicio());
        atual.setHoraFim(cand.getHoraFim());
        atual.setExigePreparo(cand.isExigePreparo());
        atual.setUrgente(cand.isUrgente());
        atual.setJustificativaUrgencia(cand.getJustificativaUrgencia());
        atual.setSoftwareTerceiros(cand.isSoftwareTerceiros());
        atual.setStatus(novo);
        atual.setLembreteProfessorEnviado(false);
        atual.setLembreteTecnicoEnviado(false);
        atual.getRecursos().clear();
        atual.getRecursos().addAll(cand.getRecursos());
        atual.setAtualizadaEm(Instant.now());
        persistir(atual);

        auditoria.registrar(ator, "Reserva", atual.getId(), "ALTERAR", antes, resumo(atual));
        if (statusAntes == StatusReserva.CONFIRMADA && mudouLocalOuHorario) {
            String msg = "A aula prática de " + atual.getTurma().getDisciplina().getNome() + " mudou para "
                + descricao(atual) + ".";
            notificacoes.notificarTodos(alunosDaTurma(atual.getTurma()), "Mudança de ensalamento", msg);
            notificacoes.notificarTodos(atual.getLaboratorio().getTecnicos(), "Reserva alterada", msg);
        }
        if (novo == StatusReserva.PENDENTE_ANALISE) avisarAnalise(atual);
        notificacoes.notificar(atual.getSolicitante(), "Reserva alterada", "Sua reserva foi atualizada: " + descricao(atual)
            + ". Situação: " + novo + ".");
        return ReservaRes.of(atual);
    }

    // ------------------------------------------------------------------ cancelamento (RF17, RN09)

    @Transactional
    public ReservaRes cancelar(Long id, String motivo, Usuario ator) {
        Reserva r = buscar(id);
        exigirGestao(r, ator);
        return ReservaRes.of(cancelarInterno(r, motivo, ator));
    }

    /** Usado também pelo bloqueio de agenda e pela inativação, após verificação de permissão. */
    @Transactional
    public Reserva cancelarInterno(Reserva r, String motivo, Usuario ator) {
        exigirMotivo(motivo);
        if (!r.ocupaHorario()) throw ApiException.regra("A reserva já está encerrada (" + r.getStatus() + ").");
        boolean confirmada = r.getStatus() == StatusReserva.CONFIRMADA;
        boolean tardio = confirmada
            && Duration.between(Tempo.agora(), r.inicio()).toHours() < params.getLimiteCancelamentoTardioHoras();
        String antes = resumo(r);
        r.setStatus(StatusReserva.CANCELADA);
        r.setMotivoDecisao(motivo.trim());
        r.setCancelamentoTardio(tardio);
        r.setAtualizadaEm(Instant.now());
        reservas.save(r);
        auditoria.registrar(ator, "Reserva", r.getId(), tardio ? "CANCELAR_TARDIO" : "CANCELAR", antes, resumo(r));

        String msg = "Reserva cancelada: " + descricao(r) + ". Motivo: " + motivo.trim();
        if (!r.getSolicitante().getId().equals(ator.getId()))
            notificacoes.notificar(r.getSolicitante(), "Reserva cancelada", msg);
        if (confirmada) {
            notificacoes.notificarTodos(r.getLaboratorio().getTecnicos(),
                tardio ? "Cancelamento tardio" : "Reserva cancelada", msg);
            notificacoes.notificarTodos(alunosDaTurma(r.getTurma()), "Aula prática cancelada", msg);
        }
        return r;
    }

    // ------------------------------------------------------------------ análise (RF15, RF16, RF18)

    @Transactional
    public ReservaRes confirmar(Long id, Usuario ator) {
        Reserva r = buscar(id);
        exigirStatus(r, StatusReserva.PENDENTE_ANALISE);
        exigirAnalise(r, ator);
        if (r.isSoftwareTerceiros())
            throw ApiException.regra("A reserva usa software de terceiros: encaminhe à TI antes de confirmar (RN06).");
        decidir(r, StatusReserva.CONFIRMADA, null, ator, "CONFIRMAR");
        notificacoes.notificar(r.getSolicitante(), "Reserva confirmada", "Sua reserva foi confirmada: " + descricao(r) + ".");
        ordemDePreparo(r);
        return ReservaRes.of(r);
    }

    @Transactional
    public ReservaRes recusar(Long id, String motivo, Usuario ator) {
        Reserva r = buscar(id);
        exigirStatus(r, StatusReserva.PENDENTE_ANALISE);
        exigirAnalise(r, ator);
        exigirMotivo(motivo);
        decidir(r, StatusReserva.RECUSADA, motivo.trim(), ator, "RECUSAR");
        notificacoes.notificar(r.getSolicitante(), "Reserva recusada",
            "Sua reserva foi recusada: " + descricao(r) + ". Motivo: " + motivo.trim());
        return ReservaRes.of(r);
    }

    @Transactional
    public ReservaRes encaminharTi(Long id, Usuario ator) {
        Reserva r = buscar(id);
        exigirStatus(r, StatusReserva.PENDENTE_ANALISE);
        exigirAnalise(r, ator);
        if (!r.isSoftwareTerceiros())
            throw ApiException.regra("Esta reserva não usa software de terceiros.");
        decidir(r, StatusReserva.PENDENTE_TI, null, ator, "ENCAMINHAR_TI");
        notificacoes.notificarTodos(usuarios.findByPerfilAndAtivoTrue(Perfil.ANALISTA_TI), "Validação de software pendente",
            "Validar a instalação de software de terceiros para " + descricao(r) + ".");
        return ReservaRes.of(r);
    }

    @Transactional
    public ReservaRes validarTi(Long id, boolean aprovado, String parecer, Usuario ator) {
        Reserva r = buscar(id);
        exigirStatus(r, StatusReserva.PENDENTE_TI);
        if (!aprovado) exigirMotivo(parecer);
        ValidacaoTi v = new ValidacaoTi();
        v.setReserva(r);
        v.setAnalista(ator);
        v.setResultado(aprovado ? ResultadoValidacao.APROVADA : ResultadoValidacao.REPROVADA);
        v.setParecer(parecer);
        v.setValidadaEm(Instant.now());
        validacoes.save(v);
        decidir(r, aprovado ? StatusReserva.CONFIRMADA : StatusReserva.RECUSADA,
            aprovado ? null : "Reprovada pela TI: " + parecer.trim(), ator, aprovado ? "TI_APROVAR" : "TI_REPROVAR");
        if (aprovado) {
            notificacoes.notificar(r.getSolicitante(), "Reserva confirmada", "Sua reserva foi confirmada após validação da TI: " + descricao(r) + ".");
            ordemDePreparo(r);
        } else {
            notificacoes.notificar(r.getSolicitante(), "Reserva recusada", "A TI reprovou o software solicitado para "
                + descricao(r) + ". Parecer: " + parecer.trim());
        }
        return ReservaRes.of(r);
    }

    // ------------------------------------------------------------------ consultas (RF09)

    @Transactional(readOnly = true)
    public List<ReservaRes> listar(Usuario ator, StatusReserva status) {
        List<Reserva> base = switch (ator.getPerfil()) {
            case PROFESSOR -> reservas.findByTurma_Professor_IdOrderByDataDescHoraInicioAsc(ator.getId());
            case TECNICO -> {
                List<Long> ids = labs.findAll().stream()
                    .filter(l -> l.tecnicoResponsavel(ator) || l.getTecnicos().isEmpty())
                    .map(Laboratorio::getId).toList();
                yield ids.isEmpty() ? List.<Reserva>of() : reservas.findByLaboratorio_IdInOrderByDataDescHoraInicioAsc(ids);
            }
            case COORDENADOR -> reservas.findAllByOrderByDataDescHoraInicioAsc();
            case ANALISTA_TI -> reservas.findAllByOrderByDataDescHoraInicioAsc().stream()
                .filter(Reserva::isSoftwareTerceiros).toList();
            default -> throw ApiException.proibido("Seu perfil não consulta reservas.");
        };
        return base.stream().filter(r -> status == null || r.getStatus() == status).map(ReservaRes::of).toList();
    }

    @Transactional(readOnly = true)
    public ReservaRes obter(Long id, Usuario ator) {
        Reserva r = buscar(id);
        boolean ok = switch (ator.getPerfil()) {
            case PROFESSOR -> r.getTurma().getProfessor().getId().equals(ator.getId());
            case TECNICO -> r.getLaboratorio().tecnicoResponsavel(ator) || r.getLaboratorio().getTecnicos().isEmpty()
                || r.getCriadaPor().getId().equals(ator.getId());
            case COORDENADOR, ANALISTA_TI -> true;
            default -> false;
        };
        if (!ok) throw ApiException.proibido("Você não tem acesso a esta reserva.");
        return ReservaRes.of(r);
    }

    // ------------------------------------------------------------------ rotinas agendadas (RN10, RN12)

    @Transactional
    public void rotinas() {
        LocalDateTime agora = Tempo.agora();
        LocalDate hoje = agora.toLocalDate();

        for (Reserva r : reservas.findByStatusInAndDataLessThanEqual(
                List.of(StatusReserva.PENDENTE_ANALISE, StatusReserva.PENDENTE_TI), hoje)) {
            if (r.inicio().isAfter(agora)) continue;
            r.setStatus(StatusReserva.EXPIRADA);
            r.setMotivoDecisao("A aula iniciou sem decisão (RN12).");
            r.setAtualizadaEm(Instant.now());
            auditoria.registrar(null, "Reserva", r.getId(), "EXPIRAR", null, resumo(r));
            notificacoes.notificar(r.getSolicitante(), "Reserva expirada",
                "Sua reserva expirou sem decisão: " + descricao(r) + ".");
        }
        for (Reserva r : reservas.findByStatusInAndDataLessThanEqual(List.of(StatusReserva.CONFIRMADA), hoje)) {
            if (!r.fim().isAfter(agora)) { r.setStatus(StatusReserva.CONCLUIDA); r.setAtualizadaEm(Instant.now()); }
        }
        for (Reserva r : reservas.findByStatusAndDataBetween(StatusReserva.CONFIRMADA, hoje, hoje.plusDays(7))) {
            long horas = Duration.between(agora, r.inicio()).toHours();
            if (!r.isLembreteProfessorEnviado() && horas >= 0 && horas <= params.getLembreteProfessorHoras()) {
                r.setLembreteProfessorEnviado(true);
                notificacoes.notificar(r.getSolicitante(), "Lembrete de reserva", "Lembrete: " + descricao(r) + ".");
            }
            if (!r.isLembreteTecnicoEnviado()
                    && r.inicio().isBefore(Tempo.somarDiasUteis(agora, params.getAntecedenciaOrdemPreparoDiasUteis()))) {
                r.setLembreteTecnicoEnviado(true);
                notificacoes.notificarTodos(r.getLaboratorio().getTecnicos(), "Lembrete de preparo",
                    "Preparar o laboratório: " + descricao(r) + ".");
            }
        }
    }

    // ------------------------------------------------------------------ internos

    private Reserva buscar(Long id) {
        return reservas.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Reserva não encontrada."));
    }

    private Reserva candidata(ReservaReq req, Usuario ator) {
        if (req.turmaId() == null || req.laboratorioId() == null)
            throw ApiException.invalido("Informe turma e laboratório.");
        Turma turma = turmas.findById(req.turmaId()).orElseThrow(() -> ApiException.invalido("Turma não encontrada."));
        Laboratorio lab = labs.findById(req.laboratorioId()).orElseThrow(() -> ApiException.invalido("Laboratório não encontrado."));
        Usuario professor = turma.getProfessor();
        if (ator.getPerfil() == Perfil.PROFESSOR && !professor.getId().equals(ator.getId()))
            throw ApiException.proibido("Você só pode reservar para as suas turmas.");

        Reserva r = new Reserva();
        r.setLaboratorio(lab);
        r.setTurma(turma);
        r.setSolicitante(professor);
        r.setCriadaPor(ator);
        r.setData(req.data());
        r.setHoraInicio(req.horaInicio());
        r.setHoraFim(req.horaFim());
        r.setExigePreparo(Boolean.TRUE.equals(req.exigePreparo()));
        r.setUrgente(Boolean.TRUE.equals(req.urgente()));
        r.setJustificativaUrgencia(req.justificativaUrgencia());
        r.setOrigem(ator.getPerfil() == Perfil.TECNICO
            ? (req.origem() != null ? req.origem() : OrigemReserva.PRESENCIAL) : OrigemReserva.PORTAL);

        Map<Long, Integer> itens = new LinkedHashMap<>();
        if (req.recursos() != null)
            for (ItemReq it : req.recursos())
                if (it.recursoId() != null && it.quantidade() != null && it.quantidade() > 0)
                    itens.merge(it.recursoId(), it.quantidade(), Integer::sum);
        boolean software = false;
        for (Map.Entry<Long, Integer> e : itens.entrySet()) {
            Recurso rec = recursos.findById(e.getKey()).orElseThrow(() -> ApiException.e("E05", 422, "Recurso inexistente."));
            r.getRecursos().add(new ItemRecurso(rec, e.getValue()));
            software |= rec.isSoftwareTerceiros();
        }
        r.setSoftwareTerceiros(software);
        return r;
    }

    /** Ordem das validações conforme a Figura 10: E01, E02, E03, E04, E05. */
    private void validar(Reserva r, Long ignorarId) {
        LocalDateTime agora = Tempo.agora();
        politica.validarDados(r, agora);
        politica.validarAntecedencia(r, agora);
        politica.validarCapacidade(r);
        Set<StatusReserva> oc = StatusReserva.OCUPANTES;
        List<Reserva> doLab = reservas.findByLaboratorio_IdAndDataAndStatusIn(r.getLaboratorio().getId(), r.getData(), oc);
        List<Reserva> daTurma = reservas.findByTurma_IdAndDataAndStatusIn(r.getTurma().getId(), r.getData(), oc);
        List<Reserva> doProf = reservas.findByTurma_Professor_IdAndDataAndStatusIn(r.getTurma().getProfessor().getId(), r.getData(), oc);
        List<BloqueioAgenda> bl = bloqueios.findByLaboratorio_IdAndData(r.getLaboratorio().getId(), r.getData());
        politica.validarConflitos(r, ignorarId, doLab, daTurma, doProf, bl);
        politica.validarRecursos(r);
    }

    /** Gravação com exclusão mútua garantida pela restrição do SGBD (RNF04, E06). */
    private void persistir(Reserva r) {
        try {
            reservas.saveAndFlush(r);
        } catch (DataIntegrityViolationException ex) {
            String causa = ex.getMostSpecificCause().getMessage();
            if (causa != null && causa.contains("reserva_sem_sobreposicao"))
                throw ApiException.e("E06", 409, "O horário acabou de ser ocupado por outro usuário. Escolha outro horário.");
            throw ex;
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw ApiException.e("E06", 409, "A reserva foi alterada por outro usuário. Recarregue e tente novamente.");
        }
    }

    private void decidir(Reserva r, StatusReserva novo, String motivo, Usuario ator, String acao) {
        String antes = resumo(r);
        r.setStatus(novo);
        if (motivo != null) r.setMotivoDecisao(motivo);
        r.setAtualizadaEm(Instant.now());
        reservas.save(r);
        auditoria.registrar(ator, "Reserva", r.getId(), acao, antes, resumo(r));
    }

    private void exigirStatus(Reserva r, StatusReserva esperado) {
        if (r.getStatus() != esperado)
            throw ApiException.regra("Operação inválida: a reserva está em " + r.getStatus() + " (esperado " + esperado + ").");
    }

    private void exigirMotivo(String motivo) {
        if (motivo == null || motivo.trim().length() < 3) throw ApiException.invalido("Informe o motivo (mínimo de 3 caracteres).");
    }

    /** Professor: só as próprias; técnico: labs sob responsabilidade ou reservas que registrou; coordenador: todas. */
    private void exigirGestao(Reserva r, Usuario ator) {
        boolean ok = switch (ator.getPerfil()) {
            case PROFESSOR -> r.getTurma().getProfessor().getId().equals(ator.getId());
            case TECNICO -> r.getLaboratorio().tecnicoResponsavel(ator) || r.getLaboratorio().getTecnicos().isEmpty()
                || r.getCriadaPor().getId().equals(ator.getId());
            case COORDENADOR -> true;
            default -> false;
        };
        if (!ok) throw ApiException.proibido("Você não pode alterar esta reserva.");
    }

    private void exigirAnalise(Reserva r, Usuario ator) {
        if (ator.getPerfil() == Perfil.COORDENADOR) return;
        if (ator.getPerfil() != Perfil.TECNICO) throw ApiException.proibido("Somente técnico ou coordenador analisa reservas.");
        if (r.isUrgente()) throw ApiException.proibido("Reservas de urgência só podem ser decididas pelo coordenador (RN08).");
        Laboratorio l = r.getLaboratorio();
        if (!l.tecnicoResponsavel(ator) && !l.getTecnicos().isEmpty())
            throw ApiException.proibido("Você não é responsável por este laboratório.");
    }

    private void avisarCriacao(Reserva r) {
        if (r.getStatus() == StatusReserva.CONFIRMADA) {
            notificacoes.notificar(r.getSolicitante(), "Reserva confirmada", "Sua reserva foi confirmada: " + descricao(r) + ".");
            ordemDePreparo(r);
        } else {
            notificacoes.notificar(r.getSolicitante(), "Reserva recebida", "Sua reserva está em análise: " + descricao(r)
                + ". Você será avisado da decisão.");
            avisarAnalise(r);
        }
    }

    private void avisarAnalise(Reserva r) {
        String msg = (r.isUrgente() ? "URGÊNCIA - " : "") + "Reserva pendente de análise: " + descricao(r)
            + (r.isUrgente() ? ". Justificativa: " + r.getJustificativaUrgencia() : "");
        if (r.isUrgente()) notificacoes.notificarTodos(usuarios.findByPerfilAndAtivoTrue(Perfil.COORDENADOR), "Urgência a aprovar", msg);
        else notificacoes.notificarTodos(r.getLaboratorio().getTecnicos(), "Reserva pendente de análise", msg);
    }

    private void ordemDePreparo(Reserva r) {
        StringBuilder sb = new StringBuilder("Ordem de preparo: " + descricao(r));
        if (!r.getRecursos().isEmpty()) {
            sb.append(". Recursos: ");
            r.getRecursos().forEach(i -> sb.append(i.getRecurso().getNome()).append(" x").append(i.getQuantidade()).append("; "));
        }
        notificacoes.notificarTodos(r.getLaboratorio().getTecnicos(), "Ordem de preparo", sb.toString());
    }

    private List<Usuario> alunosDaTurma(Turma t) {
        return matriculas.findByTurma_Id(t.getId()).stream().map(Matricula::getAluno).toList();
    }

    private String descricao(Reserva r) {
        return r.getLaboratorio().getIdentificacao() + ", " + r.getData().format(DF) + " das " + r.getHoraInicio()
            + " às " + r.getHoraFim() + " (turma " + r.getTurma().getCodigo() + ")";
    }

    private String resumo(Reserva r) {
        return "status=" + r.getStatus() + "; lab=" + r.getLaboratorio().getIdentificacao() + "; data=" + r.getData()
            + "; " + r.getHoraInicio() + "-" + r.getHoraFim() + "; turma=" + r.getTurma().getCodigo()
            + "; urgente=" + r.isUrgente();
    }
}
