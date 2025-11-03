package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.UsuarioContrato;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.UsuarioEliminado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioContratoRepository extends JpaRepository<UsuarioContrato, Long> {

    // Buscar por número de contrato único
    Optional<UsuarioContrato> findByNumeroContrato(String numeroContrato);

    // Buscar todos los contratos de un usuario activo
    List<UsuarioContrato> findByUsuario(Usuario usuario);

    // Buscar todos los contratos de un usuario eliminado
    List<UsuarioContrato> findByUsuarioEliminado(UsuarioEliminado usuarioEliminado);

    // Verificar si existe un número de contrato ya asignado
    boolean existsByNumeroContrato(String numeroContrato);

    // ✅ Verificar si un usuario activo ya tiene asignado un número de contrato
    boolean existsByUsuarioAndNumeroContrato(Usuario usuario, String numeroContrato);

    // ✅ Verificar si un usuario activo ya tiene asignado un contrato específico
    boolean existsByUsuarioAndContrato_Id(Usuario usuario, Long contratoId);

    // ✅ Verificar si un número de contrato pertenece a otro registro (útil para validación en actualización)
    boolean existsByNumeroContratoAndIdNot(String numeroContrato, Long id);
}
