package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.*;
import jakarta.validation.constraints.*;

import java.time.*;
import java.util.*;

/** Objetos de transferência (entrada e saída) da API REST. */
public final class Dtos {
    private Dtos() {}

    public record ErroRes(String codigo, String mensagem) {}

    public record LoginReq(@NotBlank String email, @NotBlank String senha) {}
    public record LoginRes(String token, UsuarioRes usuario) {}

    public record UsuarioReq(@NotBlank String nome, @NotBlank @Email String email, String senha,
                             @NotNull Perfil perfil, String telefone, String matricula,
                             Boolean consenteWhatsapp, Boolean ativo) {}
    public record UsuarioRes(Long id, String nome, String email, Perfil perfil, boolean ativo,
                             String telefone, String matricula, boolean consenteWhatsapp) {
        public static UsuarioRes of(Usuario u) {
            return new UsuarioRes(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(),
                u.getTelefone(), u.getMatricula(), u.isConsenteWhatsapp());
        }
    }

    public record RecursoReq(@NotBlank String nome, @NotNull TipoRecurso tipo, Boolean softwareTerceiros, String versao) {}
    public record RecursoRes(Long id, String nome, TipoRecurso tipo, boolean softwareTerceiros, String versao) {
        public static RecursoRes of(Recurso r) {
            return new RecursoRes(r.getId(), r.getNome(), r.getTipo(), r.isSoftwareTerceiros(), r.getVersao());
        }
    }

    public record LabRecursoReq(@NotNull Long recursoId, @NotNull @Min(0) Integer quantidade) {}
    public record LabRecursoRes(Long recursoId, String nome, TipoRecurso tipo, boolean softwareTerceiros, int quantidade) {}

    public record LabReq(@NotBlank String identificacao, String localizacao,
                         @NotNull @Min(1) Integer capacidadeMaxima,
                         @NotNull LocalTime horaAbertura, @NotNull LocalTime horaFechamento,
                         StatusLaboratorio status, Boolean exigeAcompanhamentoTecnico,
                         List<Long> tecnicoIds, List<LabRecursoReq> recursos) {}
    public record LabRes(Long id, String identificacao, String localizacao, int capacidadeMaxima,
                         LocalTime horaAbertura, LocalTime horaFechamento, StatusLaboratorio status,
                         boolean exigeAcompanhamentoTecnico, List<UsuarioRes> tecnicos, List<LabRecursoRes> recursos) {
        public static LabRes of(Laboratorio l) {
            return new LabRes(l.getId(), l.getIdentificacao(), l.getLocalizacao(), l.getCapacidadeMaxima(),
                l.getHoraAbertura(), l.getHoraFechamento(), l.getStatus(), l.isExigeAcompanhamentoTecnico(),
                l.getTecnicos().stream().map(UsuarioRes::of).sorted(Comparator.comparing(UsuarioRes::nome)).toList(),
                l.getRecursos().stream().map(r -> new LabRecursoRes(r.getRecurso().getId(), r.getRecurso().getNome(),
                    r.getRecurso().getTipo(), r.getRecurso().isSoftwareTerceiros(), r.getQuantidade()))
                    .sorted(Comparator.comparing(LabRecursoRes::nome)).toList());
        }
    }
    public record StatusReq(@NotNull StatusLaboratorio status) {}

    public record TurmaRes(Long id, String codigo, String periodoLetivo, String disciplinaCodigo,
                           String disciplinaNome, Long professorId, String professorNome, int numeroAlunos) {
        public static TurmaRes of(Turma t) {
            return new TurmaRes(t.getId(), t.getCodigo(), t.getPeriodoLetivo(), t.getDisciplina().getCodigo(),
                t.getDisciplina().getNome(), t.getProfessor().getId(), t.getProfessor().getNome(), t.getNumeroAlunos());
        }
    }

    public record ItemReq(Long recursoId, Integer quantidade) {}
    public record ItemRes(Long recursoId, String nome, int quantidade) {}

    public record ReservaReq(Long turmaId, Long laboratorioId, LocalDate data, LocalTime horaInicio,
                             LocalTime horaFim, List<ItemReq> recursos, Boolean exigePreparo,
                             Boolean urgente, String justificativaUrgencia, OrigemReserva origem) {}
    public record ReservaRes(Long id, Long laboratorioId, String laboratorio, Long turmaId, String turma,
                             String disciplina, int numeroAlunos, Long professorId, String professor,
                             LocalDate data, LocalTime horaInicio, LocalTime horaFim, StatusReserva status,
                             OrigemReserva origem, boolean urgente, String justificativaUrgencia,
                             boolean exigePreparo, boolean softwareTerceiros, String motivoDecisao,
                             boolean cancelamentoTardio, String criadaPor, List<ItemRes> recursos) {
        public static ReservaRes of(Reserva r) {
            return new ReservaRes(r.getId(), r.getLaboratorio().getId(), r.getLaboratorio().getIdentificacao(),
                r.getTurma().getId(), r.getTurma().getCodigo(), r.getTurma().getDisciplina().getNome(),
                r.getTurma().getNumeroAlunos(), r.getSolicitante().getId(), r.getSolicitante().getNome(),
                r.getData(), r.getHoraInicio(), r.getHoraFim(), r.getStatus(), r.getOrigem(), r.isUrgente(),
                r.getJustificativaUrgencia(), r.isExigePreparo(), r.isSoftwareTerceiros(), r.getMotivoDecisao(),
                r.isCancelamentoTardio(), r.getCriadaPor().getNome(),
                r.getRecursos().stream().map(i -> new ItemRes(i.getRecurso().getId(), i.getRecurso().getNome(), i.getQuantidade()))
                    .sorted(Comparator.comparing(ItemRes::nome)).toList());
        }
    }
    public record MotivoReq(String motivo) {}
    public record ValidacaoTiReq(Boolean aprovado, String parecer) {}

    public record BloqueioReq(@NotNull Long laboratorioId, @NotNull LocalDate data, @NotNull LocalTime horaInicio,
                              @NotNull LocalTime horaFim, @NotBlank String motivo, Boolean cancelarConflitantes) {}
    public record BloqueioRes(Long id, Long laboratorioId, String laboratorio, LocalDate data,
                              LocalTime horaInicio, LocalTime horaFim, String motivo, String criadoPor) {
        public static BloqueioRes of(BloqueioAgenda b) {
            return new BloqueioRes(b.getId(), b.getLaboratorio().getId(), b.getLaboratorio().getIdentificacao(),
                b.getData(), b.getHoraInicio(), b.getHoraFim(), b.getMotivo(), b.getCriadoPor().getNome());
        }
    }

    public record Ocupacao(String tipo, String descricao, LocalTime horaInicio, LocalTime horaFim, String status) {}
    public record DisponibilidadeRes(LabRes laboratorio, List<Ocupacao> ocupacoes) {}

    public record NotificacaoRes(Long id, CanalNotificacao canal, String assunto, String mensagem,
                                 StatusEnvio status, Instant criadaEm) {
        public static NotificacaoRes of(Notificacao n) {
            return new NotificacaoRes(n.getId(), n.getCanal(), n.getAssunto(), n.getMensagem(), n.getStatus(), n.getCriadaEm());
        }
    }

    public record AuditoriaRes(Long id, String autor, String entidade, Long entidadeId, String acao,
                               String dadosAnteriores, String dadosPosteriores, Instant ocorridoEm) {
        public static AuditoriaRes of(RegistroAuditoria a) {
            return new AuditoriaRes(a.getId(), a.getAutorNome(), a.getEntidade(), a.getEntidadeId(), a.getAcao(),
                a.getDadosAnteriores(), a.getDadosPosteriores(), a.getOcorridoEm());
        }
    }

    public record LinhaOcupacao(Long laboratorioId, String laboratorio, double horasDisponiveis,
                                double horasOcupadas, double taxaOciosidade) {}
    public record RelatorioRes(LocalDate inicio, LocalDate fim, List<LinhaOcupacao> laboratorios,
                               double horasDisponiveis, double horasOcupadas, double taxaOciosidade,
                               long totalReservas, long urgentes, long cancelamentosTardios,
                               long canceladas, long recusadas, long expiradas) {}

    public record ImportacaoRes(int linhasLidas, int turmasProcessadas, int matriculasCriadas, List<String> erros) {}
}
