package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "notificacao")
@Getter @Setter @NoArgsConstructor
public class Notificacao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "destinatario_id") private Usuario destinatario;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CanalNotificacao canal;
    @Column(nullable = false) private String assunto;
    @Column(nullable = false, columnDefinition = "TEXT") private String mensagem;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusEnvio status = StatusEnvio.PENDENTE;
    private int tentativas;
    @Column(nullable = false) private Instant criadaEm;
    private Instant enviadaEm;
}
