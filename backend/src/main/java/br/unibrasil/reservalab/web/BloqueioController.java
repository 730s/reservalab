package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.Usuario;
import br.unibrasil.reservalab.service.BloqueioService;
import br.unibrasil.reservalab.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bloqueios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('COORDENADOR')")
public class BloqueioController {
    private final BloqueioService service;

    @GetMapping
    public List<BloqueioRes> listar() { return service.listar(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioRes criar(@Valid @RequestBody BloqueioReq req, @AuthenticationPrincipal Usuario u) { return service.criar(req, u); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id, @AuthenticationPrincipal Usuario u) { service.remover(id, u); }
}
