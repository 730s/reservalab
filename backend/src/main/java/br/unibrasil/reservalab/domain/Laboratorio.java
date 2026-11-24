package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/** Entidade de domínio: guarda estado e responde só a regras que dependem dele (D04). */
@Entity @Table(name = "laboratorio")
@Getter @Setter @NoArgsConstructor
public class Laboratorio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true) private String identificacao;
    private String localizacao;
    @Column(nullable = false) private Integer capacidadeMaxima;
    @Column(nullable = false) private LocalTime horaAbertura;
    @Column(nullable = false) private LocalTime horaFechamento;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private StatusLaboratorio status = StatusLaboratorio.ATIVO;
    private boolean exigeAcompanhamentoTecnico;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "laboratorio_tecnico",
        joinColumns = @JoinColumn(name = "laboratorio_id"),
        inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    private Set<Usuario> tecnicos = new HashSet<>();

    @OneToMany(mappedBy = "laboratorio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<RecursoDoLaboratorio> recursos = new HashSet<>();

    public boolean comportaTurma(Turma t) { return t.getNumeroAlunos() <= capacidadeMaxima; }

    public int quantidadeDe(Long recursoId) {
        return recursos.stream().filter(r -> r.getRecurso().getId().equals(recursoId))
            .mapToInt(RecursoDoLaboratorio::getQuantidade).sum();
    }

    public boolean possuiRecurso(Long recursoId, int qtd) { return quantidadeDe(recursoId) >= qtd; }

    public boolean estaOperacional() { return status == StatusLaboratorio.ATIVO; }

    public boolean tecnicoResponsavel(Usuario u) {
        return tecnicos.stream().anyMatch(t -> t.getId().equals(u.getId()));
    }
}
