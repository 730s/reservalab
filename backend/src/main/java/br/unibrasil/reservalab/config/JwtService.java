package br.unibrasil.reservalab.config;

import br.unibrasil.reservalab.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttlMinutos;

    public JwtService(@Value("${reservalab.jwt.secret}") String secret,
                      @Value("${reservalab.jwt.ttl-minutes}") long ttlMinutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutos = ttlMinutos;
    }

    public String gerar(Usuario u) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
            .subject(String.valueOf(u.getId()))
            .claim("perfil", u.getPerfil().name())
            .issuedAt(new Date(agora))
            .expiration(new Date(agora + ttlMinutos * 60_000))
            .signWith(key)
            .compact();
    }

    public Claims ler(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
