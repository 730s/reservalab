package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.Usuario;
import br.unibrasil.reservalab.service.*;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Notificações do usuário, trilha de auditoria (RF28) e relatórios (RF29). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConsultasController {
    private final NotificacaoService notificacoes;
    private final AuditoriaService auditoria;
    private final RelatorioService relatorios;

    @GetMapping("/notificacoes")
    public List<NotificacaoRes> minhasNotificacoes(@AuthenticationPrincipal Usuario u) {
        return notificacoes.doUsuario(u.getId()).stream().map(NotificacaoRes::of).toList();
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasAnyRole('COORDENADOR','ANALISTA_TI')")
    public List<AuditoriaRes> auditoria() { return auditoria.recentes().stream().map(AuditoriaRes::of).toList(); }

    @GetMapping("/relatorios/ocupacao")
    @PreAuthorize("hasAnyRole('COORDENADOR','TECNICO','DIRECAO')")
    public RelatorioRes ocupacao(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
                                 @AuthenticationPrincipal Usuario u) {
        return relatorios.ocupacao(inicio, fim, u);
    }
}
