package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.Localidad;
import com.Tapia.ProyectoResidencia.Model.Municipio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LocalidadRepository extends JpaRepository<Localidad, Long> {

    Optional<Localidad> findByNombre(String nombre);

    boolean existsByMunicipioAndNumeroLocalidad(Municipio municipio, String numeroLocalidad);
    boolean existsByMunicipioAndNumeroLocalidadAndIdNot(Municipio municipio, String numeroLocalidad, Long id);

    boolean existsByMunicipioAndNombre(Municipio municipio, String nombre);
    boolean existsByMunicipioAndNombreAndIdNot(Municipio municipio, String nombre, Long id);

    List<Localidad> findByMunicipio_Id(String municipioId);
}
