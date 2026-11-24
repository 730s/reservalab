package br.unibrasil.reservalab.web;

import br.unibrasil.reservalab.domain.Usuario;
import br.unibrasil.reservalab.service.AuthService;
import br.unibrasil.reservalab.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;

    @PostMapping("/login")
    public LoginRes login(@Valid @RequestBody LoginReq req) { return auth.login(req); }

    /** Renovação da sessão enquanto o usuário estiver ativo (expira em 30 min de inatividade). */
    @PostMapping("/renovar")
    public LoginRes renovar(@AuthenticationPrincipal Usuario u) { return auth.renovar(u); }

    @GetMapping("/me")
    public UsuarioRes me(@AuthenticationPrincipal Usuario u) { return UsuarioRes.of(u); }
}
