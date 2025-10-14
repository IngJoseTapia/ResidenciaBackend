package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Enum.NotificationTemplate;
import com.Tapia.ProyectoResidencia.Model.Notification;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByTemplateAndDestinatarios_Usuario(NotificationTemplate template, Usuario usuario);
    void deleteByTemplateAndDestinatarios_Usuario(NotificationTemplate template, Usuario usuario);
    Optional<Notification> findByTemplate(NotificationTemplate template);
}
