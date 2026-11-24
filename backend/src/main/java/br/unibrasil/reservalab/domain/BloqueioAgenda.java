package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity @Table(name = "bloqueio_agenda")
@Getter @Setter @NoArgsConstructor
public class BloqueioAgenda {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "laboratorio_id") private Laboratorio laboratorio;
    @Column(nullable = false) private LocalDate data;
    @Column(nullable = false) private LocalTime horaInicio;
    @Column(nullable = false) private LocalTime horaFim;
    @Column(nullable = false) private String motivo;
    @ManyToOne(optional = false) @JoinColumn(name = "criado_por_id") private Usuario criadoPor;

    public boolean sobrepoe(LocalTime ini, LocalTime fim) {
        return horaInicio.isBefore(fim) && ini.isBefore(horaFim);
    }
}
