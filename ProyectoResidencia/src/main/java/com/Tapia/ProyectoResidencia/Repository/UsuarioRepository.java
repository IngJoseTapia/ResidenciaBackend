package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Status;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario,Long> {
    // Buscar usuario por correo
    Optional<Usuario> findByCorreo(String correo);

    // Verificar si un correo ya existe
    boolean existsByCorreo(String correo);

    // Buscar usuario por rol
    List<Usuario> findByRol(Rol rol);

    // Buscar usuarios por status
    Page<Usuario> findByStatus(Status status, Pageable pageable);

}
