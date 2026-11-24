package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "turma")
@Getter @Setter @NoArgsConstructor
public class Turma {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String codigo;
    private String periodoLetivo;
    @ManyToOne(optional = false) @JoinColumn(name = "disciplina_id") private Disciplina disciplina;
    @ManyToOne(optional = false) @JoinColumn(name = "professor_id") private Usuario professor;
    @Column(nullable = false) private Integer numeroAlunos = 0;
}
