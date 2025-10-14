package com.Tapia.ProyectoResidencia.Repository;

import com.Tapia.ProyectoResidencia.Enum.NotificationTemplate;
import com.Tapia.ProyectoResidencia.Model.NotificacionUsuario;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificacionUsuarioRepository extends JpaRepository<NotificacionUsuario, Long> {
    List<NotificacionUsuario> findByUsuarioId(Long usuarioId);
    Optional<NotificacionUsuario> findByNotificacionIdAndUsuarioId(Long notificacionId, Long usuarioId);
    List<NotificacionUsuario> findByUsuarioAndNotificacion_TemplateAndResueltaFalse(
            Usuario usuario,
            NotificationTemplate template
    );
    List<NotificacionUsuario> findByUsuarioAndNotificacion_Template(
            Usuario usuario,
            NotificationTemplate template
    );
    // Todas las notificaciones de un usuario
    List<NotificacionUsuario> findByUsuarioOrderByNotificacion_TipoAscFechaLecturaDesc(Usuario usuario);

    // Opcional: solo notificaciones no leídas
    List<NotificacionUsuario> findByUsuarioAndLeidaFalseOrderByNotificacion_TipoAscFechaLecturaDesc(Usuario usuario);

    //List<NotificacionUsuario> findByUsuarioAndLeidaFalse(Usuario usuario);

    Optional<NotificacionUsuario> findByIdAndUsuario(Long id, Usuario usuario);
}
