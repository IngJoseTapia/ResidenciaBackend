package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.Vocalia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VocaliaRepository extends JpaRepository<Vocalia, Long> {
    Optional<Vocalia> findByAbreviatura(String abreviatura);
    boolean existsByAbreviatura(String abreviatura);
}
