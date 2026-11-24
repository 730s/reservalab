package br.unibrasil.reservalab.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "registro_auditoria")
@Getter @Setter @NoArgsConstructor
public class RegistroAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long autorId;
    private String autorNome;
    @Column(nullable = false) private String entidade;
    private Long entidadeId;
    @Column(nullable = false) private String acao;
    @Column(columnDefinition = "TEXT") private String dadosAnteriores;
    @Column(columnDefinition = "TEXT") private String dadosPosteriores;
    @Column(nullable = false) private Instant ocorridoEm;
}
