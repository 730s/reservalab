package br.unibrasil.reservalab.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Parâmetros P01..P13 do Quadro 12 (D12): configuráveis por application.yml / variáveis de ambiente. */
@Component
@ConfigurationProperties(prefix = "reservalab.parametros")
@Getter @Setter
public class ParametrosOperacionais {
    private int antecedenciaComPreparoDiasUteis = 3;
    private int antecedenciaSemPreparoHoras = 2;
    private int antecedenciaSoftwareTerceirosDiasUteis = 5;
    private int antecedenciaOrdemPreparoDiasUteis = 2;
    private int lembreteProfessorHoras = 24;
    private int prazoAnaliseDiasUteis = 1;
    private int prazoValidacaoTiDiasUteis = 2;
    private int prazoUrgenciaHorasUteis = 4;
    private int limiteCancelamentoTardioHoras = 24;
    private int notificacaoAlunosMinutos = 5;
    private int retencaoAnos = 5;
    private int tentativasLogin = 5;
    private int bloqueioLoginMinutos = 15;
    private int duracaoMinimaMinutos = 50;
    private int justificativaUrgenciaMinCaracteres = 20;
}
