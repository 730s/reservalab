package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "disciplina")
@Getter @Setter @NoArgsConstructor
public class Disciplina {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String codigo;
    @Column(nullable = false) private String nome;
    private Integer cargaHoraria;
}
