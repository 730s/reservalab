package br.unibrasil.reservalab.repository;

import br.unibrasil.reservalab.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findTop100ByDestinatario_IdOrderByCriadaEmDesc(Long destinatarioId);
    List<Notificacao> findTop50ByStatusAndTentativasLessThanOrderByCriadaEmAsc(StatusEnvio status, int max);
}
