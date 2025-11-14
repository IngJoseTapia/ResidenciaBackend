package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.Seccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeccionRepository extends JpaRepository<Seccion, Long> {

    boolean existsByNumeroSeccionAndAnioAndAsignacionZoreAre_Id(String numeroSeccion, String anio,Long asignacionZoreAreId);
    boolean existsByNumeroSeccionAndAnioAndAsignacionZoreAre_IdAndIdNot(String numeroSeccion, String anio, Long asignacionZoreAreId, Long id);
}
