package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.Usuario;
import br.unibrasil.reservalab.service.TurmaService;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TurmaController {
    private final TurmaService service;

    @GetMapping("/turmas")
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR')")
    public List<TurmaRes> turmas(@AuthenticationPrincipal Usuario u) { return service.listar(u); }

    @PostMapping("/turmas/importar")
    @PreAuthorize("hasRole('COORDENADOR')")
    public ImportacaoRes importar(@RequestParam("arquivo") MultipartFile arquivo, @AuthenticationPrincipal Usuario u) throws IOException {
        return service.importar(arquivo, u);
    }

    @GetMapping("/ensalamento")
    @PreAuthorize("hasAnyRole('ALUNO','PROFESSOR','COORDENADOR')")
    public List<ReservaRes> ensalamento(@AuthenticationPrincipal Usuario u) { return service.ensalamento(u); }
}
