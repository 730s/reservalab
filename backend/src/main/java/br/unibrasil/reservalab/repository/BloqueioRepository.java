package br.unibrasil.reservalab.repository;

import br.unibrasil.reservalab.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface BloqueioRepository extends JpaRepository<BloqueioAgenda, Long> {
    List<BloqueioAgenda> findByLaboratorio_IdAndData(Long labId, LocalDate data);
    List<BloqueioAgenda> findByData(LocalDate data);
    List<BloqueioAgenda> findByDataGreaterThanEqualOrderByDataAscHoraInicioAsc(LocalDate data);
    List<BloqueioAgenda> findByDataBetween(LocalDate de, LocalDate ate);
}
