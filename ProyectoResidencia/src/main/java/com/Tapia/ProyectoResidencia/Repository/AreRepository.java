package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.Are;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AreRepository extends JpaRepository<Are, Long> {

    Optional<Are> findByNumeracionAndAnio(int numeracion, String anio);

    boolean existsByNumeracionAndAnio(int numeracion, String anio);

    boolean existsByNumeracionAndAnioAndIdNot(int numeracion, String anio, Long id);

    Optional<Are> findByUsuarioAndAnio(Usuario usuario, String anio);

    // Listar todas las ARE de un año específico
    List<Are> findByAnio(String anio);
}
