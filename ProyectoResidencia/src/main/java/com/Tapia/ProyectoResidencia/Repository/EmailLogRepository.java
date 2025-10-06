package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
}
