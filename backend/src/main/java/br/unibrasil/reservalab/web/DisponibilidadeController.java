package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.service.DisponibilidadeService;
import br.unibrasil.reservalab.web.Dtos.DisponibilidadeRes;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/disponibilidade")
@RequiredArgsConstructor
public class DisponibilidadeController {
    private final DisponibilidadeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR','TECNICO','COORDENADOR')")
    public List<DisponibilidadeRes> consultar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Integer capacidadeMinima,
            @RequestParam(required = false) Long recursoId) {
        return service.consultar(data, capacidadeMinima, recursoId);
    }
}
