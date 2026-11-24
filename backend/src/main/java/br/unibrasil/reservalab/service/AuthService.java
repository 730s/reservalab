package br.unibrasil.reservalab.service;

import br.unibrasil.reservalab.config.JwtService;
import br.unibrasil.reservalab.config.ParametrosOperacionais;
import br.unibrasil.reservalab.domain.*;
import br.unibrasil.reservalab.repository.UsuarioRepository;
import br.unibrasil.reservalab.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** UC01 (RF01, RNF14): autenticação com bloqueio após tentativas falhas. */
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final ParametrosOperacionais params;
    private final AuditoriaService auditoria;

    private static ApiException credenciais() { return ApiException.e("E00", 401, "E-mail ou senha inválidos."); }

    @Transactional(noRollbackFor = ApiException.class)
    public LoginRes login(LoginReq req) {
        Usuario u = repo.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        if (u == null) throw credenciais();
        if (u.getBloqueadoAte() != null && u.getBloqueadoAte().isAfter(Instant.now()))
            throw ApiException.e("E00", 423, "Conta temporariamente bloqueada por excesso de tentativas. Tente novamente em alguns minutos.");
        if (!u.isAtivo() || !encoder.matches(req.senha(), u.getSenhaHash())) {
            u.setTentativasFalhas(u.getTentativasFalhas() + 1);
            if (u.getTentativasFalhas() >= params.getTentativasLogin()) {
                u.setBloqueadoAte(Instant.now().plus(params.getBloqueioLoginMinutos(), ChronoUnit.MINUTES));
                u.setTentativasFalhas(0);
                auditoria.registrar(u, "Usuario", u.getId(), "BLOQUEIO_LOGIN", null, "bloqueado por tentativas falhas");
            }
            repo.save(u);
            throw credenciais();
        }
        u.setTentativasFalhas(0);
        u.setBloqueadoAte(null);
        repo.save(u);
        return new LoginRes(jwt.gerar(u), UsuarioRes.of(u));
    }

    public LoginRes renovar(Usuario u) { return new LoginRes(jwt.gerar(u), UsuarioRes.of(u)); }
}
