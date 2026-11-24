package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.HashSet;
import java.util.Set;

@Entity @Table(name = "reserva")
@Getter @Setter @NoArgsConstructor
public class Reserva {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "laboratorio_id") private Laboratorio laboratorio;
    @ManyToOne(optional = false) @JoinColumn(name = "turma_id") private Turma turma;
    @ManyToOne(optional = false) @JoinColumn(name = "solicitante_id") private Usuario solicitante;
    @ManyToOne(optional = false) @JoinColumn(name = "criada_por_id") private Usuario criadaPor;
    @Column(nullable = false) private LocalDate data;
    @Column(nullable = false) private LocalTime horaInicio;
    @Column(nullable = false) private LocalTime horaFim;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusReserva status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private OrigemReserva origem = OrigemReserva.PORTAL;
    private boolean urgente;
    private String justificativaUrgencia;
    private boolean exigePreparo;
    private boolean softwareTerceiros;
    private String motivoDecisao;
    private boolean cancelamentoTardio;
    private boolean lembreteProfessorEnviado;
    private boolean lembreteTecnicoEnviado;
    @Column(nullable = false) private Instant criadaEm;
    @Column(nullable = false) private Instant atualizadaEm;
    @Version private Long versao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reserva_recurso", joinColumns = @JoinColumn(name = "reserva_id"))
    private Set<ItemRecurso> recursos = new HashSet<>();

    /** Intervalos [início, fim) que se interceptam (RN01). */
    public boolean sobrepoe(LocalTime ini, LocalTime fim) {
        return horaInicio.isBefore(fim) && ini.isBefore(horaFim);
    }

    public LocalDateTime inicio() { return LocalDateTime.of(data, horaInicio); }
    public LocalDateTime fim() { return LocalDateTime.of(data, horaFim); }
    public boolean ocupaHorario() { return status.ocupaHorario(); }
}
