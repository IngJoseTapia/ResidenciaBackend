package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.AsignacionZoreAre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AsignacionZoreAreRepository extends JpaRepository<AsignacionZoreAre, Long> {

    // ✅ Evita duplicados de asignaciones entre el mismo ZORE y ARE
    boolean existsByZoreIdAndAreId(Long zoreId, Long areId);

    // ✅ Si necesitas buscar una asignación específica
    Optional<AsignacionZoreAre> findByZoreIdAndAreId(Long zoreId, Long areId);

    boolean existsByAreIdAndAnio(Long areId, String anio);

    boolean existsByZoreIdAndAreIdAndIdNot(Long zoreId, Long areId, Long id);

    List<AsignacionZoreAre> findByAnio(String anio);
}
