package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    // Buscar contrato por código único
    Optional<Contrato> findByCodigo(String codigo);

    // Verificar existencia por código
    boolean existsByCodigo(String codigo);

    // Verificar duplicado al actualizar (excluyendo el registro actual)
    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
