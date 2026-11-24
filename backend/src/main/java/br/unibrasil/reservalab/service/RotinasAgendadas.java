package br.unibrasil.reservalab.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Expiração de pendências (RN12), conclusão de reservas e lembretes (RN10). */
@Component
@RequiredArgsConstructor
public class RotinasAgendadas {
    private final ReservaService reservas;

    @Scheduled(fixedDelay = 60_000, initialDelay = 20_000)
    public void executar() { reservas.rotinas(); }
}
