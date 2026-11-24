package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.RecursoRepository;
import br.unibrasil.reservalab.service.AuditoriaService;
import br.unibrasil.reservalab.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/** UC04 (RF07): catálogo de recursos e softwares. */
@RestController
@RequestMapping("/api/recursos")
@RequiredArgsConstructor
public class RecursoController {
    private final RecursoRepository repo;
    private final AuditoriaService auditoria;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR','ANALISTA_TI')")
    public List<RecursoRes> listar() {
        return repo.findAll().stream().sorted(Comparator.comparing(Recurso::getNome)).map(RecursoRes::of).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    @PreAuthorize("hasAnyRole('TECNICO','COORDENADOR')")
    public RecursoRes criar(@Valid @RequestBody RecursoReq req, @AuthenticationPrincipal Usuario u) {
        Recurso r = new Recurso();
        aplicar(r, req);
        repo.save(r);
        auditoria.registrar(u, "Recurso", r.getId(), "CRIAR", null, r.getNome());
        return RecursoRes.of(r);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasAnyRole('TECNICO','COORDENADOR')")
    public RecursoRes editar(@PathVariable Long id, @Valid @RequestBody RecursoReq req, @AuthenticationPrincipal Usuario u) {
        Recurso r = repo.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Recurso não encontrado."));
        String antes = r.getNome() + " / " + r.getTipo() + " / terceiros=" + r.isSoftwareTerceiros();
        aplicar(r, req);
        repo.save(r);
        auditoria.registrar(u, "Recurso", r.getId(), "EDITAR", antes, r.getNome());
        return RecursoRes.of(r);
    }

    private void aplicar(Recurso r, RecursoReq req) {
        r.setNome(req.nome().trim());
        r.setTipo(req.tipo());
        r.setSoftwareTerceiros(req.tipo() == TipoRecurso.SOFTWARE && Boolean.TRUE.equals(req.softwareTerceiros()));
        r.setVersao(req.versao());
    }
}
