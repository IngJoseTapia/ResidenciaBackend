package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Integer> {

}
