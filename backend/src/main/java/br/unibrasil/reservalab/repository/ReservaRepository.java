package br.unibrasil.reservalab.repository;

import br.unibrasil.reservalab.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByLaboratorio_IdAndDataAndStatusIn(Long labId, LocalDate data, Collection<StatusReserva> st);
    List<Reserva> findByTurma_IdAndDataAndStatusIn(Long turmaId, LocalDate data, Collection<StatusReserva> st);
    List<Reserva> findByTurma_Professor_IdAndDataAndStatusIn(Long profId, LocalDate data, Collection<StatusReserva> st);
    List<Reserva> findByDataAndStatusIn(LocalDate data, Collection<StatusReserva> st);

    List<Reserva> findAllByOrderByDataDescHoraInicioAsc();
    List<Reserva> findByTurma_Professor_IdOrderByDataDescHoraInicioAsc(Long professorId);
    List<Reserva> findByLaboratorio_IdInOrderByDataDescHoraInicioAsc(Collection<Long> labIds);

    @Query("select r from Reserva r where r.turma.id in :turmas and r.status in :st order by r.data, r.horaInicio")
    List<Reserva> findDasTurmas(@Param("turmas") Collection<Long> turmas, @Param("st") Collection<StatusReserva> st);

    @Query("select r from Reserva r where r.laboratorio.id = :lab and r.data >= :hoje and r.status in :st order by r.data, r.horaInicio")
    List<Reserva> futurasDoLaboratorio(@Param("lab") Long lab, @Param("hoje") LocalDate hoje, @Param("st") Collection<StatusReserva> st);

    @Query("select r from Reserva r where r.laboratorio.id = :lab and r.data = :data and r.status in :st")
    List<Reserva> doLaboratorioNoDia(@Param("lab") Long lab, @Param("data") LocalDate data, @Param("st") Collection<StatusReserva> st);

    List<Reserva> findByStatusInAndDataLessThanEqual(Collection<StatusReserva> st, LocalDate data);
    List<Reserva> findByStatusAndDataBetween(StatusReserva st, LocalDate de, LocalDate ate);

    @Query("select r from Reserva r where r.data between :de and :ate")
    List<Reserva> noPeriodo(@Param("de") LocalDate de, @Param("ate") LocalDate ate);
}
