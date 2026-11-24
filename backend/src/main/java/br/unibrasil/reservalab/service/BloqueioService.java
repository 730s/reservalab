package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.*;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** UC08 (RF10, RN14): bloqueio de agenda, privativo do coordenador. */
@Service
@RequiredArgsConstructor
public class BloqueioService {
    private final BloqueioRepository bloqueios;
    private final LaboratorioRepository labs;
    private final ReservaRepository reservas;
    private final ReservaService reservaService;
    private final AuditoriaService auditoria;

    @Transactional
    public BloqueioRes criar(BloqueioReq req, Usuario ator) {
        Laboratorio lab = labs.findById(req.laboratorioId()).orElseThrow(() -> ApiException.invalido("Laboratório não encontrado."));
        if (!req.horaFim().isAfter(req.horaInicio())) throw ApiException.invalido("O horário final deve ser posterior ao inicial.");
        List<Reserva> conflitantes = reservas.doLaboratorioNoDia(lab.getId(), req.data(), StatusReserva.OCUPANTES).stream()
            .filter(r -> r.sobrepoe(req.horaInicio(), req.horaFim())).toList();
        if (!conflitantes.isEmpty() && !Boolean.TRUE.equals(req.cancelarConflitantes())) {
            String lista = conflitantes.stream().map(r -> "#" + r.getId() + " " + r.getTurma().getCodigo() + " "
                + r.getHoraInicio() + "-" + r.getHoraFim()).reduce((a, b) -> a + "; " + b).orElse("");
            throw ApiException.e("E04", 409, "Há " + conflitantes.size() + " reserva(s) no intervalo: " + lista
                + ". Cancele/remaneje ou marque a opção de cancelar as reservas conflitantes.");
        }
        for (Reserva r : conflitantes)
            reservaService.cancelarInterno(r, "Bloqueio de agenda: " + req.motivo().trim(), ator);

        BloqueioAgenda b = new BloqueioAgenda();
        b.setLaboratorio(lab);
        b.setData(req.data());
        b.setHoraInicio(req.horaInicio());
        b.setHoraFim(req.horaFim());
        b.setMotivo(req.motivo().trim());
        b.setCriadoPor(ator);
        bloqueios.save(b);
        auditoria.registrar(ator, "BloqueioAgenda", b.getId(), "CRIAR", null,
            lab.getIdentificacao() + " " + b.getData() + " " + b.getHoraInicio() + "-" + b.getHoraFim() + " " + b.getMotivo());
        return BloqueioRes.of(b);
    }

    @Transactional
    public void remover(Long id, Usuario ator) {
        BloqueioAgenda b = bloqueios.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Bloqueio não encontrado."));
        bloqueios.delete(b);
        auditoria.registrar(ator, "BloqueioAgenda", id, "REMOVER", b.getMotivo(), null);
    }

    @Transactional(readOnly = true)
    public List<BloqueioRes> listar() {
        return bloqueios.findByDataGreaterThanEqualOrderByDataAscHoraInicioAsc(Tempo.hoje()).stream().map(BloqueioRes::of).toList();
    }
}
