package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.*;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/** UC11 (RF29, IND01): TO = (Hdisp - Hocup) / Hdisp x 100. */
@Service
@RequiredArgsConstructor
public class RelatorioService {
    private final LaboratorioRepository labs;
    private final ReservaRepository reservas;
    private final BloqueioRepository bloqueios;

    @Transactional(readOnly = true)
    public RelatorioRes ocupacao(LocalDate inicio, LocalDate fim, Usuario ator) {
        if (fim.isBefore(inicio)) throw ApiException.invalido("A data final deve ser posterior à inicial.");
        if (Duration.between(inicio.atStartOfDay(), fim.atStartOfDay()).toDays() > 366)
            throw ApiException.invalido("Período máximo de 366 dias.");

        long diasLetivos = inicio.datesUntil(fim.plusDays(1)).filter(d -> d.getDayOfWeek() != DayOfWeek.SUNDAY).count();
        List<Reserva> doPeriodo = reservas.noPeriodo(inicio, fim);
        List<BloqueioAgenda> bl = bloqueios.findByDataBetween(inicio, fim);

        List<Laboratorio> considerados = labs.findAll().stream()
            .filter(l -> l.getStatus() != StatusLaboratorio.INATIVO)
            .filter(l -> ator.getPerfil() != Perfil.TECNICO || l.tecnicoResponsavel(ator) || l.getTecnicos().isEmpty())
            .sorted(Comparator.comparing(Laboratorio::getIdentificacao)).toList();

        List<LinhaOcupacao> linhas = new ArrayList<>();
        double totalDisp = 0, totalOcup = 0;
        for (Laboratorio l : considerados) {
            double horasDia = Duration.between(l.getHoraAbertura(), l.getHoraFechamento()).toMinutes() / 60.0;
            double bloqueadas = bl.stream().filter(b -> b.getLaboratorio().getId().equals(l.getId()))
                .mapToDouble(b -> Duration.between(b.getHoraInicio(), b.getHoraFim()).toMinutes() / 60.0).sum();
            double disp = Math.max(0, diasLetivos * horasDia - bloqueadas);
            double ocup = doPeriodo.stream().filter(r -> r.getLaboratorio().getId().equals(l.getId()))
                .filter(r -> r.getStatus() == StatusReserva.CONFIRMADA || r.getStatus() == StatusReserva.CONCLUIDA)
                .mapToDouble(r -> Duration.between(r.getHoraInicio(), r.getHoraFim()).toMinutes() / 60.0).sum();
            linhas.add(new LinhaOcupacao(l.getId(), l.getIdentificacao(), arred(disp), arred(ocup), taxa(disp, ocup)));
            totalDisp += disp;
            totalOcup += ocup;
        }
        Set<Long> ids = new HashSet<>();
        considerados.forEach(l -> ids.add(l.getId()));
        List<Reserva> escopo = doPeriodo.stream().filter(r -> ids.contains(r.getLaboratorio().getId())).toList();
        return new RelatorioRes(inicio, fim, linhas, arred(totalDisp), arred(totalOcup), taxa(totalDisp, totalOcup),
            escopo.size(),
            escopo.stream().filter(Reserva::isUrgente).count(),
            escopo.stream().filter(Reserva::isCancelamentoTardio).count(),
            escopo.stream().filter(r -> r.getStatus() == StatusReserva.CANCELADA).count(),
            escopo.stream().filter(r -> r.getStatus() == StatusReserva.RECUSADA).count(),
            escopo.stream().filter(r -> r.getStatus() == StatusReserva.EXPIRADA).count());
    }

    private static double arred(double v) { return Math.round(v * 10.0) / 10.0; }

    private static double taxa(double disp, double ocup) {
        return disp <= 0 ? 0 : arred(Math.max(0, (disp - ocup) / disp * 100.0));
    }
}
