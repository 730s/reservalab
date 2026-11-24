package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.*;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/** RF08 (UC06): disponibilidade em tempo real, sempre lida do banco (sem cache). */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {
    private final LaboratorioRepository labs;
    private final ReservaRepository reservas;
    private final BloqueioRepository bloqueios;

    @Transactional(readOnly = true)
    public List<DisponibilidadeRes> consultar(LocalDate data, Integer capacidadeMinima, Long recursoId) {
        List<Reserva> doDia = reservas.findByDataAndStatusIn(data, StatusReserva.OCUPANTES);
        List<BloqueioAgenda> bloqueiosDoDia = bloqueios.findByData(data);
        return labs.findAll().stream()
            .filter(l -> l.getStatus() != StatusLaboratorio.INATIVO)
            .filter(l -> capacidadeMinima == null || l.getCapacidadeMaxima() >= capacidadeMinima)
            .filter(l -> recursoId == null || l.quantidadeDe(recursoId) > 0)
            .sorted(Comparator.comparing(Laboratorio::getIdentificacao))
            .map(l -> {
                List<Ocupacao> oc = new ArrayList<>();
                doDia.stream().filter(r -> r.getLaboratorio().getId().equals(l.getId()))
                    .forEach(r -> oc.add(new Ocupacao("RESERVA",
                        r.getTurma().getCodigo() + " - " + r.getTurma().getDisciplina().getNome(),
                        r.getHoraInicio(), r.getHoraFim(), r.getStatus().name())));
                bloqueiosDoDia.stream().filter(b -> b.getLaboratorio().getId().equals(l.getId()))
                    .forEach(b -> oc.add(new Ocupacao("BLOQUEIO", b.getMotivo(), b.getHoraInicio(), b.getHoraFim(), "BLOQUEADO")));
                oc.sort(Comparator.comparing(Ocupacao::horaInicio));
                return new DisponibilidadeRes(LabRes.of(l), oc);
            }).toList();
    }
}
