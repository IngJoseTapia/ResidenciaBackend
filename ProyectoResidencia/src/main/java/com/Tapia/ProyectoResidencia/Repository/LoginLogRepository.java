package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
    // No necesitamos métodos especiales por ahora, solo guardar los logs
}
