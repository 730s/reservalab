package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "recurso")
@Getter @Setter @NoArgsConstructor
public class Recurso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String nome;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private TipoRecurso tipo;
    private boolean softwareTerceiros;
    private String versao;
}
