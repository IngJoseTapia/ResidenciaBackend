package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Model.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {
    Page<SystemLog> findAllByOrderByFechaActividadDesc(Pageable pageable);
    Page<SystemLog> findByCorreoContainingIgnoreCaseOrderByFechaActividadDesc(String correo, Pageable pageable);
    Page<SystemLog> findByTipoEventoOrderByFechaActividadDesc(Evento tipoEvento, Pageable pageable);
    Page<SystemLog> findByResultadoOrderByFechaActividadDesc(Resultado resultado, Pageable pageable);
}
