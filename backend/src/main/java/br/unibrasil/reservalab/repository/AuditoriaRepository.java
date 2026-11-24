package br.unibrasil.reservalab.repository;

import br.unibrasil.reservalab.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface AuditoriaRepository extends JpaRepository<RegistroAuditoria, Long> {
    List<RegistroAuditoria> findTop200ByOrderByOcorridoEmDesc();
}
