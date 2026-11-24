package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;

/** Recurso requisitado numa reserva (RecursoRequisitado do diagrama). */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ItemRecurso {
    @ManyToOne(optional = false, fetch = FetchType.EAGER) @JoinColumn(name = "recurso_id")
    private Recurso recurso;
    @Column(nullable = false) private Integer quantidade;
}
