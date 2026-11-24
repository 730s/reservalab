package br.unibrasil.reservalab.domain;

import java.time.*;

/** RNF12/D16: regras de calendário em America/Sao_Paulo; instantes persistidos em UTC. */
public final class Tempo {
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private Tempo() {}

    public static LocalDateTime agora() { return LocalDateTime.now(ZONA); }
    public static LocalDate hoje() { return LocalDate.now(ZONA); }

    /** Soma dias úteis (seg-sex). Feriados do calendário acadêmico: evolução futura (RF25). */
    public static LocalDateTime somarDiasUteis(LocalDateTime base, int dias) {
        LocalDateTime r = base;
        int n = 0;
        while (n < dias) {
            r = r.plusDays(1);
            DayOfWeek d = r.getDayOfWeek();
            if (d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY) n++;
        }
        return r;
    }

    public static Instant paraInstant(LocalDateTime t) { return t.atZone(ZONA).toInstant(); }
}
