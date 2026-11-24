package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "validacao_ti")
@Getter @Setter @NoArgsConstructor
public class ValidacaoTi {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "reserva_id") private Reserva reserva;
    @ManyToOne(optional = false) @JoinColumn(name = "analista_id") private Usuario analista;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ResultadoValidacao resultado;
    private String parecer;
    @Column(nullable = false) private Instant validadaEm;
}
