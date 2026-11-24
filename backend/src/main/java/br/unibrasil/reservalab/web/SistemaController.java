package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.config.ParametrosOperacionais;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SistemaController {
    private final ParametrosOperacionais params;
    public SistemaController(ParametrosOperacionais params) { this.params = params; }

    @GetMapping("/health")
    public Map<String, String> health() { return Map.of("status", "UP"); }

    /** RF25 (MVP): parâmetros vêm de arquivo de configuração; aqui apenas consulta. */
    @GetMapping("/parametros")
    @PreAuthorize("hasAnyRole('COORDENADOR','ANALISTA_TI')")
    public ParametrosOperacionais parametros() { return params; }
}
