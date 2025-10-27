package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Model.LoginLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
    Page<LoginLog> findAllByOrderByFechaActividadDesc(Pageable pageable);
    Page<LoginLog> findByTipoEventoOrderByFechaActividadDesc(Evento evento, Pageable pageable);
    Page<LoginLog> findByCorreoContainingIgnoreCaseOrderByFechaActividadDesc(String correo, Pageable pageable);
}
