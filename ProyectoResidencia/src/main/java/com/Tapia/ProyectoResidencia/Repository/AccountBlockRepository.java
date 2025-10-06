package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.AccountBlock;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Enum.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountBlockRepository extends JpaRepository<AccountBlock, Long> {
    Optional<AccountBlock> findByUsuarioAndEvento(Usuario usuario, Evento evento);
    List<AccountBlock> findByUsuario(Usuario usuario);
}
