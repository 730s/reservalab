package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "matricula")
@Getter @Setter @NoArgsConstructor
public class Matricula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "turma_id") private Turma turma;
    @ManyToOne(optional = false) @JoinColumn(name = "aluno_id") private Usuario aluno;
    private String situacao = "ATIVA";
}
