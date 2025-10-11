package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.NotificacionResponse;
import com.Tapia.ProyectoResidencia.Enum.NotificationTemplate;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.TipoNotificacion;
import com.Tapia.ProyectoResidencia.Model.*;
import com.Tapia.ProyectoResidencia.Repository.NotificationRepository;
import com.Tapia.ProyectoResidencia.Repository.NotificacionUsuarioRepository;
import com.Tapia.ProyectoResidencia.Repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificationRepository notificationRepository;
    private final NotificacionUsuarioRepository notificacionUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    @Transactional
    public void createNotificationSystem(Usuario usuario, NotificationTemplate template) {
        if (existeNotificacionUsuario(usuario, template)) {
            return; // Evita duplicado
        }

        String titulo;
        String mensaje;

        switch (template) {
            case GENERAR_CONTRASENA:
                titulo = "Configura tu contraseña";
                mensaje = "Debes generar tu contraseña para poder usar todas las funcionalidades del sistema.";
                crearNotificacionSistema(usuario, titulo, mensaje, template);
                break;
            case PROFILE_INCOMPLETE:
                titulo = "Completa tu perfil";
                mensaje = "Aún tienes campos obligatorios pendientes en tu perfil.";
                crearNotificacionSistema(usuario, titulo, mensaje, template);
                break;
            case NEW_MESSAGE:
                titulo = "Nuevo mensaje recibido";
                mensaje = "Tienes un nuevo mensaje en tu bandeja de entrada.";
                crearNotificacionSistema(usuario, titulo, mensaje, template);
                break;
            case CUSTOM_ADMIN:
                titulo = "Notificación administrativa";
                mensaje = "El administrador ha enviado un aviso importante.";
                crearNotificacionSistema(usuario, titulo, mensaje, template);
                break;
        }
    }

    private void crearNotificacionSistema(Usuario usuario, String titulo, String mensaje, NotificationTemplate template) {
        if (notificationRepository.existsByTemplateAndDestinatarios_Usuario(template, usuario)) {
            return; // Ya existe una notificación de este tipo para este usuario, no crear otra
        }

        // 1. Crear la notificación
        Notification notification = new Notification();
        notification.setTipo(TipoNotificacion.SISTEMA);
        notification.setTitulo(titulo);
        notification.setMensaje(mensaje);
        notification.setTemplate(template); // <-- aquí guardas el tipo
        notification.setEmisor(null);
        notification.setRolDestino(null);
        notification.setFechaCreacion(LocalDateTime.now());

        // 2. Crear la relación con el usuario destinatario
        NotificacionUsuario relacion = new NotificacionUsuario();
        relacion.setNotificacion(notification);
        relacion.setUsuario(usuario);
        relacion.setLeida(false);

        // 3. Vincular ambas entidades
        notification.getDestinatarios().add(relacion);
        usuario.getNotificacionesRecibidas().add(relacion);

        // 4. Guardar la notificación (esto también guarda la relación por cascade)
        notificationRepository.save(notification);
    }

    public boolean existeNotificacionUsuario(Usuario usuario, NotificationTemplate template) {
        return notificationRepository.existsByTemplateAndDestinatarios_Usuario(template, usuario);
    }

    @Transactional
    public void eliminarNotificacionesPorTemplate(Usuario usuario, NotificationTemplate template) {
        List<NotificacionUsuario> relaciones = notificacionUsuarioRepository
                .findByUsuarioAndNotificacion_Template(usuario, template);

        if (relaciones.isEmpty()) return;

        // Extraer las notificaciones asociadas
        List<Notification> notificaciones = relaciones.stream()
                .map(NotificacionUsuario::getNotificacion)
                .distinct()
                .toList();

        // Eliminar primero las relaciones
        notificacionUsuarioRepository.deleteAll(relaciones);

        // Luego eliminar las notificaciones si ya no tienen destinatarios
        for (Notification n : notificaciones) {
            if (n.getDestinatarios().isEmpty()) {
                notificationRepository.delete(n);
            }
        }
    }

    @Transactional
    public void marcarNotificacionResuelta(Usuario usuario, NotificationTemplate template) {
        // Buscar todas las notificaciones activas de este tipo para este usuario
        List<NotificacionUsuario> relaciones = notificacionUsuarioRepository
                .findByUsuarioAndNotificacion_TemplateAndResueltaFalse(usuario, template);

        if (relaciones.isEmpty()) return;

        for (NotificacionUsuario rel : relaciones) {
            rel.setResuelta(true);
            rel.setFechaLectura(LocalDateTime.now());
        }

        notificacionUsuarioRepository.saveAll(relaciones);
    }

    public List<NotificacionUsuario> getNotificacionesUsuario(Usuario usuario, boolean soloNoLeidas) {
        List<NotificacionUsuario> relaciones = soloNoLeidas
                ? notificacionUsuarioRepository.findByUsuarioAndLeidaFalseOrderByNotificacion_TipoAscFechaLecturaDesc(usuario)
                : notificacionUsuarioRepository.findByUsuarioOrderByNotificacion_TipoAscFechaLecturaDesc(usuario);

        // Ordenar primero las notificaciones de ADMIN, luego SISTEMA
        return relaciones.stream()
                .sorted(Comparator.comparing((NotificacionUsuario nu) -> nu.getNotificacion().getTipo())
                        .reversed()) // ADMIN > SISTEMA
                .collect(Collectors.toList());
    }

    public List<NotificacionResponse> getNotificacionesPorCorreo(String correo, boolean soloNoLeidas) {
        Usuario usuario = usuarioService.getUsuarioEntityByCorreo(correo);

        // Usamos tus métodos ya ordenados por tipo y fecha
        List<NotificacionUsuario> notificaciones = soloNoLeidas
                ? notificacionUsuarioRepository.findByUsuarioAndLeidaFalseOrderByNotificacion_TipoAscFechaLecturaDesc(usuario)
                : notificacionUsuarioRepository.findByUsuarioOrderByNotificacion_TipoAscFechaLecturaDesc(usuario);

        return notificaciones.stream()
                .map(nu -> new NotificacionResponse(
                        nu.getNotificacion().getTitulo(),
                        nu.getNotificacion().getMensaje(),
                        nu.getNotificacion().getTipo(),
                        nu.isLeida(),
                        nu.getFechaLectura(),
                        nu.getNotificacion().getFechaCreacion()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Crear notificación generada por el sistema
     */
    @Transactional
    public Notification crearNotificacionSistema(String titulo, String mensaje, Rol rolDestino) {
        Notification notificacion = new Notification();
        notificacion.setTipo(TipoNotificacion.SISTEMA);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setRolDestino(rolDestino);

        notificationRepository.save(notificacion);

        // Asignar automáticamente a todos los usuarios del rol
        List<Usuario> usuarios = usuarioRepository.findByRol(rolDestino);
        for (Usuario usuario : usuarios) {
            NotificacionUsuario nu = new NotificacionUsuario();
            nu.setNotificacion(notificacion);
            nu.setUsuario(usuario);
            nu.setLeida(false);
            nu.setFechaLectura(null);
            notificacionUsuarioRepository.save(nu);
        }

        return notificacion;
    }

    /**
     * Crear notificación generada por un administrador
     */
    @Transactional
    public Notification crearNotificacionAdmin(Usuario emisor, String titulo, String mensaje, List<Long> destinatariosIds, Rol rolDestino) {
        Notification notificacion = new Notification();
        notificacion.setTipo(TipoNotificacion.ADMIN);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setEmisor(emisor);
        notificacion.setRolDestino(rolDestino);

        notificationRepository.save(notificacion);

        // Si se envía a un rol completo
        if (rolDestino != null) {
            List<Usuario> usuarios = usuarioRepository.findByRol(rolDestino);
            for (Usuario usuario : usuarios) {
                crearRelacionNotificacionUsuario(notificacion, usuario);
            }
        }

        // Si se envía a usuarios específicos
        if (destinatariosIds != null && !destinatariosIds.isEmpty()) {
            List<Usuario> usuarios = usuarioRepository.findAllById(destinatariosIds);
            for (Usuario usuario : usuarios) {
                crearRelacionNotificacionUsuario(notificacion, usuario);
            }
        }

        return notificacion;
    }

    /**
     * Marcar notificación como leída
     */
    @Transactional
    public void marcarComoLeida(Long notificacionId, Long usuarioId) {
        NotificacionUsuario nu = notificacionUsuarioRepository.findByNotificacionIdAndUsuarioId(notificacionId, usuarioId)
                .orElseThrow(() -> new RuntimeException("No se encontró la notificación para este usuario"));
        nu.setLeida(true);
        nu.setFechaLectura(LocalDateTime.now());
        notificacionUsuarioRepository.save(nu);
    }

    /**
     * Obtener notificaciones de un usuario
     */
    public List<NotificacionUsuario> obtenerNotificacionesUsuario(Long usuarioId) {
        return notificacionUsuarioRepository.findByUsuarioId(usuarioId);
    }

    /**
     * Método auxiliar para crear relación Notificación-Usuario
     */
    private void crearRelacionNotificacionUsuario(Notification notificacion, Usuario usuario) {
        NotificacionUsuario nu = new NotificacionUsuario();
        nu.setNotificacion(notificacion);
        nu.setUsuario(usuario);
        nu.setLeida(false);
        notificacionUsuarioRepository.save(nu);
    }
}
