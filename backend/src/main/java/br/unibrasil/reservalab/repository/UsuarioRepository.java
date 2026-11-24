package br.unibrasil.reservalab.repository;

import br.unibrasil.reservalab.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailIgnoreCase(String email);
    List<Usuario> findByPerfilAndAtivoTrue(Perfil perfil);
    List<Usuario> findByPerfil(Perfil perfil);
}
