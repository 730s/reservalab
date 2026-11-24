package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.service.LaboratorioService;
import br.unibrasil.reservalab.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/laboratorios")
@RequiredArgsConstructor
public class LaboratorioController {
    private final LaboratorioService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR','ANALISTA_TI')")
    public List<LabRes> listar(@RequestParam(required = false) String q,
                               @RequestParam(required = false) Integer capacidadeMinima,
                               @RequestParam(required = false) Long recursoId,
                               @RequestParam(required = false) StatusLaboratorio status) {
        return service.listar(q, capacidadeMinima, recursoId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR','ANALISTA_TI')")
    public LabRes obter(@PathVariable Long id) { return service.obter(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDENADOR')")
    public LabRes criar(@Valid @RequestBody LabReq req, @AuthenticationPrincipal Usuario u) { return service.criar(req, u); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    public LabRes editar(@PathVariable Long id, @Valid @RequestBody LabReq req, @AuthenticationPrincipal Usuario u) {
        return service.editar(id, req, u);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('COORDENADOR')")
    public LabRes inativar(@PathVariable Long id, @AuthenticationPrincipal Usuario u) { return service.inativar(id, u); }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECNICO','COORDENADOR')")
    public LabRes status(@PathVariable Long id, @Valid @RequestBody StatusReq req, @AuthenticationPrincipal Usuario u) {
        return service.alterarStatus(id, req.status(), u);
    }

    @PutMapping("/{id}/recursos")
    @PreAuthorize("hasAnyRole('TECNICO','COORDENADOR')")
    public LabRes recursos(@PathVariable Long id, @RequestBody List<LabRecursoReq> itens, @AuthenticationPrincipal Usuario u) {
        return service.definirRecursos(id, itens, u);
    }
}
