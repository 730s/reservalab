package br.unibrasil.reservalab.domain;

import br.unibrasil.reservalab.config.ParametrosOperacionais;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.List;

/**
 * Serviço de domínio (D04): decide validações de reserva (RN01..RN08, RN18) a partir de dados
 * já carregados. Não acessa banco; a garantia final contra concorrência é a restrição no SGBD.
 */
@Component
public class PoliticaDeReserva {
    private final ParametrosOperacionais p;

    public PoliticaDeReserva(ParametrosOperacionais p) { this.p = p; }

    private static ApiException e01(String m) { return ApiException.e("E01", 422, m); }

    /** E01 - dados inválidos. */
    public void validarDados(Reserva r, LocalDateTime agora) {
        if (r.getLaboratorio() == null || r.getTurma() == null || r.getData() == null
                || r.getHoraInicio() == null || r.getHoraFim() == null)
            throw e01("Informe turma, laboratório, data e horários.");
        if (!r.getHoraFim().isAfter(r.getHoraInicio()))
            throw e01("O horário final deve ser posterior ao horário inicial.");
        long min = Duration.between(r.getHoraInicio(), r.getHoraFim()).toMinutes();
        if (min < p.getDuracaoMinimaMinutos())
            throw e01("Duração mínima da reserva: " + p.getDuracaoMinimaMinutos() + " minutos.");
        if (r.inicio().isBefore(agora))
            throw e01("A data e o horário da reserva já passaram.");
        if (r.getData().getDayOfWeek() == DayOfWeek.SUNDAY)
            throw e01("Domingo não é dia letivo.");
        Laboratorio l = r.getLaboratorio();
        if (r.getHoraInicio().isBefore(l.getHoraAbertura()) || r.getHoraFim().isAfter(l.getHoraFechamento()))
            throw e01("Fora do horário de funcionamento do laboratório (" + l.getHoraAbertura()
                + " às " + l.getHoraFechamento() + ").");
    }

    /** E02 - antecedência (RN07/RN08). Marca/desmarca a urgência conforme o caso. */
    public void validarAntecedencia(Reserva r, LocalDateTime agora) {
        LocalDateTime minimo;
        String descricao;
        if (r.isSoftwareTerceiros()) {
            minimo = Tempo.somarDiasUteis(agora, p.getAntecedenciaSoftwareTerceirosDiasUteis());
            descricao = p.getAntecedenciaSoftwareTerceirosDiasUteis() + " dias úteis (software de terceiros)";
        } else if (r.isExigePreparo()) {
            minimo = Tempo.somarDiasUteis(agora, p.getAntecedenciaComPreparoDiasUteis());
            descricao = p.getAntecedenciaComPreparoDiasUteis() + " dias úteis (preparo de materiais)";
        } else {
            minimo = agora.plusHours(p.getAntecedenciaSemPreparoHoras());
            descricao = p.getAntecedenciaSemPreparoHoras() + " horas";
        }
        if (!r.inicio().isBefore(minimo)) {
            r.setUrgente(false);
            r.setJustificativaUrgencia(null);
            return;
        }
        if (!r.isUrgente())
            throw ApiException.e("E02", 422, "Antecedência insuficiente (mínimo de " + descricao
                + "). Marque a reserva como urgência e informe a justificativa.");
        String j = r.getJustificativaUrgencia();
        if (j == null || j.trim().length() < p.getJustificativaUrgenciaMinCaracteres())
            throw ApiException.e("E02", 422, "Urgência exige justificativa com pelo menos "
                + p.getJustificativaUrgenciaMinCaracteres() + " caracteres.");
    }

    /** E03 - capacidade (RN03). */
    public void validarCapacidade(Reserva r) {
        if (!r.getLaboratorio().comportaTurma(r.getTurma()))
            throw ApiException.e("E03", 422, "A turma tem " + r.getTurma().getNumeroAlunos()
                + " alunos e o laboratório comporta " + r.getLaboratorio().getCapacidadeMaxima() + ".");
    }

    /** E04 - conflitos de laboratório, turma, professor e bloqueios (RN01, RN02, RN14). */
    public void validarConflitos(Reserva r, Long ignorarId, List<Reserva> doLab, List<Reserva> daTurma,
                                 List<Reserva> doProfessor, List<BloqueioAgenda> bloqueios) {
        for (Reserva o : doLab)
            if (!o.getId().equals(ignorarId) && o.sobrepoe(r.getHoraInicio(), r.getHoraFim()))
                throw ApiException.e("E04", 409, "Laboratório já ocupado das " + o.getHoraInicio() + " às " + o.getHoraFim() + ".");
        for (Reserva o : daTurma)
            if (!o.getId().equals(ignorarId) && o.sobrepoe(r.getHoraInicio(), r.getHoraFim()))
                throw ApiException.e("E04", 409, "A turma já tem reserva em " + o.getLaboratorio().getIdentificacao()
                    + " das " + o.getHoraInicio() + " às " + o.getHoraFim() + ".");
        for (Reserva o : doProfessor)
            if (!o.getId().equals(ignorarId) && o.sobrepoe(r.getHoraInicio(), r.getHoraFim()))
                throw ApiException.e("E04", 409, "O professor já está em outra reserva das "
                    + o.getHoraInicio() + " às " + o.getHoraFim() + ".");
        for (BloqueioAgenda b : bloqueios)
            if (b.sobrepoe(r.getHoraInicio(), r.getHoraFim()))
                throw ApiException.e("E04", 409, "Agenda bloqueada (" + b.getMotivo() + ") das "
                    + b.getHoraInicio() + " às " + b.getHoraFim() + ".");
    }

    /** E05 - recursos e laboratório em operação (RN05). */
    public void validarRecursos(Reserva r) {
        Laboratorio l = r.getLaboratorio();
        if (!l.estaOperacional())
            throw ApiException.e("E05", 422, "Laboratório fora de operação (" + l.getStatus() + ").");
        for (ItemRecurso it : r.getRecursos()) {
            if (!l.possuiRecurso(it.getRecurso().getId(), it.getQuantidade()))
                throw ApiException.e("E05", 422, "Recurso insuficiente: " + it.getRecurso().getNome()
                    + " (solicitado " + it.getQuantidade() + ", disponível " + l.quantidadeDe(it.getRecurso().getId()) + ").");
        }
    }

    /** D05: confirmação automática; análise humana só nas exceções. */
    public StatusReserva decidirStatusInicial(Reserva r) {
        if (r.isUrgente() || r.isSoftwareTerceiros() || r.getLaboratorio().isExigeAcompanhamentoTecnico())
            return StatusReserva.PENDENTE_ANALISE;
        return StatusReserva.CONFIRMADA;
    }
}
