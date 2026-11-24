package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.service.UsuarioService;
import br.unibrasil.reservalab.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService service;

    @GetMapping
    @PreAuthorize("hasRole('ANALISTA_TI')")
    public List<UsuarioRes> listar() { return service.listar(); }

    @GetMapping("/tecnicos")
    @PreAuthorize("hasAnyRole('COORDENADOR','ANALISTA_TI')")
    public List<UsuarioRes> tecnicos() { return service.porPerfil(Perfil.TECNICO); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ANALISTA_TI')")
    public UsuarioRes criar(@Valid @RequestBody UsuarioReq req, @AuthenticationPrincipal Usuario u) { return service.criar(req, u); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ANALISTA_TI')")
    public UsuarioRes atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioReq req, @AuthenticationPrincipal Usuario u) {
        return service.atualizar(id, req, u);
    }
}
