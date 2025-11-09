package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.Zore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ZoreRepository extends JpaRepository<Zore, Long> {

    Optional<Zore> findByNumeracionAndAnio(int numeracion, String anio);

    boolean existsByNumeracionAndAnio(int numeracion, String anio);

    boolean existsByNumeracionAndAnioAndIdNot(int numeracion, String anio, Long id);

    Optional<Zore> findByUsuarioAndAnio(Usuario usuario, String anio);

    // Listar todas las ZORE de un año específico
    List<Zore> findByAnio(String anio);
}
