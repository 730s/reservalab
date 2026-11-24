package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "usuario")
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String matricula;
    @Column(nullable = false) private String nome;
    @Column(nullable = false, unique = true) private String email;
    private String telefone;
    @Column(nullable = false) private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Perfil perfil;
    private boolean ativo = true;
    private boolean consenteWhatsapp;
    private int tentativasFalhas;
    private Instant bloqueadoAte;

    public boolean possuiPerfil(Perfil... perfis) {
        for (Perfil p : perfis) if (p == perfil) return true;
        return false;
    }
}
