package br.unibrasil.reservalab.repository;

import br.unibrasil.reservalab.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAluno_Id(Long alunoId);
    List<Matricula> findByTurma_Id(Long turmaId);
    boolean existsByTurma_IdAndAluno_Id(Long turmaId, Long alunoId);
}
