package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "recurso_laboratorio")
@Getter @Setter @NoArgsConstructor
public class RecursoDoLaboratorio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "laboratorio_id")
    private Laboratorio laboratorio;
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "recurso_id")
    private Recurso recurso;
    @Column(nullable = false) private Integer quantidade;
}
