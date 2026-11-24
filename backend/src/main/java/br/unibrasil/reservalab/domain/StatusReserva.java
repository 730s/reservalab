package br.unibrasil.reservalab.domain;

import java.util.EnumSet;
import java.util.Set;

/** Ciclo de vida da reserva (Figura 12). */
public enum StatusReserva {
    PENDENTE_ANALISE, PENDENTE_TI, CONFIRMADA, RECUSADA, CANCELADA, EXPIRADA, CONCLUIDA;

    /** Estados que retêm o horário (RN01, RN12). */
    public static final Set<StatusReserva> OCUPANTES = EnumSet.of(PENDENTE_ANALISE, PENDENTE_TI, CONFIRMADA);

    public boolean ocupaHorario() { return OCUPANTES.contains(this); }
}
