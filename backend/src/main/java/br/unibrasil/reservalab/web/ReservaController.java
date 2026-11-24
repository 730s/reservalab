package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.service.ReservaService;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
public class ReservaController {
    private final ReservaService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR','ANALISTA_TI')")
    public List<ReservaRes> listar(@AuthenticationPrincipal Usuario u, @RequestParam(required = false) StatusReserva status) {
        return service.listar(u, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR','ANALISTA_TI')")
    public ReservaRes obter(@PathVariable Long id, @AuthenticationPrincipal Usuario u) { return service.obter(id, u); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO')")
    public ReservaRes criar(@RequestBody ReservaReq req, @AuthenticationPrincipal Usuario u) { return service.solicitar(req, u); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR')")
    public ReservaRes alterar(@PathVariable Long id, @RequestBody ReservaReq req, @AuthenticationPrincipal Usuario u) {
        return service.alterar(id, req, u);
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR')")
    public ReservaRes cancelar(@PathVariable Long id, @RequestBody MotivoReq body, @AuthenticationPrincipal Usuario u) {
        return service.cancelar(id, body.motivo(), u);
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAnyRole('TECNICO','COORDENADOR')")
    public ReservaRes confirmar(@PathVariable Long id, @AuthenticationPrincipal Usuario u) { return service.confirmar(id, u); }

    @PostMapping("/{id}/recusar")
    @PreAuthorize("hasAnyRole('TECNICO','COORDENADOR')")
    public ReservaRes recusar(@PathVariable Long id, @RequestBody MotivoReq body, @AuthenticationPrincipal Usuario u) {
        return service.recusar(id, body.motivo(), u);
    }

    @PostMapping("/{id}/encaminhar-ti")
    @PreAuthorize("hasAnyRole('TECNICO','COORDENADOR')")
    public ReservaRes encaminharTi(@PathVariable Long id, @AuthenticationPrincipal Usuario u) { return service.encaminharTi(id, u); }

    @PostMapping("/{id}/validar-ti")
    @PreAuthorize("hasRole('ANALISTA_TI')")
    public ReservaRes validarTi(@PathVariable Long id, @RequestBody ValidacaoTiReq body, @AuthenticationPrincipal Usuario u) {
        return service.validarTi(id, Boolean.TRUE.equals(body.aprovado()), body.parecer(), u);
    }
}
