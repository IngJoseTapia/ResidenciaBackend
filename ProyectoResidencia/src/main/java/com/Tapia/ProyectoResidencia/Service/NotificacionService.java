package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.DTO.NotificacionResponse;
import com.Tapia.ProyectoResidencia.DTO.NotificationStatusRequest;
import com.Tapia.ProyectoResidencia.Enum.NotificationTemplate;
import com.Tapia.ProyectoResidencia.Enum.TipoNotificacion;
import com.Tapia.ProyectoResidencia.Model.*;
import com.Tapia.ProyectoResidencia.Repository.NotificationRepository;
import com.Tapia.ProyectoResidencia.Repository.NotificacionUsuarioRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.Tapia.ProyectoResidencia.Enum.NotificationTemplate.*;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificationRepository notificationRepository;
    private final NotificacionUsuarioRepository notificacionUsuarioRepository;
    private final UsuarioService usuarioService;

    private static final Map<NotificationTemplate, String[]> SYSTEM_NOTIFICATIONS = Map.of(
            GENERAR_CONTRASENA, new String[]{"Configura tu contraseña", "Debes generar tu contraseña para poder usar todas las funcionalidades del sistema."},
            PERFIL_INCOMPLETO, new String[]{"Completa tu perfil", "Actualiza tus datos personales en tu perfil."},
            NEW_MESSAGE, new String[]{"Nuevo mensaje recibido", "Tienes un nuevo mensaje en tu bandeja de entrada."},
            CUSTOM_ADMIN, new String[]{"Notificación administrativa", "El administrador ha enviado un aviso importante."}
    );

    @Transactional
    public void createNotificationSystem(Usuario usuario, NotificationTemplate template) {
        if (notificationRepository.existsByTemplateAndDestinatarios_Usuario(template, usuario)) return;

        String[] info = SYSTEM_NOTIFICATIONS.get(template);
        if (info != null) {
            crearNotificacionSistema(usuario, info[0], info[1], template);
        }
    }

    private void crearNotificacionSistema(Usuario usuario, String titulo, String mensaje, NotificationTemplate template) {
        if (notificationRepository.existsByTemplateAndDestinatarios_Usuario(template, usuario)) {
            return; // Ya existe una notificación de este tipo para este usuario, no crear otra
        }

        // 1. Crear la notificación
        Notification notification = notificationRepository
                .findByTemplate(template)
                .orElseGet(() -> {
                    Notification n = new Notification();
                    n.setTipo(TipoNotificacion.SISTEMA);
                    n.setTitulo(titulo);
                    n.setMensaje(mensaje);
                    n.setTemplate(template);
                    n.setEmisor(null);
                    n.setRolDestino(null);
                    n.setFechaCreacion(LocalDateTime.now());
                    return notificationRepository.save(n);
                });

        // 2. Crear la relación con el usuario destinatario
        NotificacionUsuario relacion = new NotificacionUsuario();
        relacion.setNotificacion(notification);
        relacion.setUsuario(usuario);
        relacion.setLeida(false);
        relacion.setResuelta(false);
        relacion.setFechaRecepcion(LocalDateTime.now());

        // 3. Vincular ambas entidades
        notification.getDestinatarios().add(relacion);
        //usuario.getNotificacionesRecibidas().add(relacion);

        // 4. Guardar la notificación (esto también guarda la relación por cascade)
        notificationRepository.save(notification);
        notificationRepository.flush(); // fuerza commit en BD
    }

    public boolean existeNotificacionUsuario(Usuario usuario, NotificationTemplate template) {
        return notificationRepository.existsByTemplateAndDestinatarios_Usuario(template, usuario);
    }

    public List<NotificacionResponse> getNotificacionesPorCorreo(String correo, boolean soloNoLeidas) {
        Usuario usuario = usuarioService.getUsuarioEntityByCorreo(correo);

        List<NotificacionUsuario> notificaciones = soloNoLeidas
                ? notificacionUsuarioRepository.findByUsuarioAndLeidaFalseOrderByNotificacion_TipoAscFechaLecturaDesc(usuario)
                : notificacionUsuarioRepository.findByUsuarioOrderByNotificacion_TipoAscFechaLecturaDesc(usuario);

        return notificaciones.stream()
                .map(nu -> new NotificacionResponse(
                        nu.getId(), // 🔹 ID de la relación NotificacionUsuario
                        nu.getNotificacion().getTitulo(),
                        nu.getNotificacion().getMensaje(),
                        nu.getNotificacion().getTipo(),
                        nu.getNotificacion().getTemplate(),
                        nu.isLeida(),
                        nu.isResuelta(),
                        nu.getFechaRecepcion(), // 🔹 ahora usamos la fecha de recepción real
                        nu.getFechaLectura()    // 🔹 si no ha sido leída, será null
                ))
                .collect(Collectors.toList());
    }


    @Transactional
    public void actualizarEstadoNotificacion(Long idRelacion, Usuario usuario, NotificationStatusRequest request) {
        if (request.leida() == null && request.resuelta() == null) {
            throw new IllegalArgumentException("Debe especificarse al menos un campo para actualizar.");
        }

        NotificacionUsuario relacion = notificacionUsuarioRepository
                .findByIdAndUsuario(idRelacion, usuario)
                .orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada o no pertenece al usuario."));

        boolean cambios = false;

        if (Boolean.TRUE.equals(request.leida()) && !relacion.isLeida()) {
            relacion.setLeida(true);
            relacion.setFechaLectura(LocalDateTime.now());
            cambios = true;
        }

        if (Boolean.TRUE.equals(request.resuelta()) && !relacion.isResuelta()) {
            relacion.setResuelta(true);
            cambios = true;
        }

        if (cambios) {
            notificacionUsuarioRepository.save(relacion);
        }
    }

    @Transactional
    public void resolverYEliminarNotificaciones(Usuario usuario, NotificationTemplate template) {
        // 1️⃣ Buscar relaciones pendientes de resolución
        List<NotificacionUsuario> relaciones =
                notificacionUsuarioRepository.findByUsuarioAndNotificacion_TemplateAndResueltaFalse(usuario, template);

        if (relaciones.isEmpty()) return;

        // 2️⃣ Marcar como resuelta y registrar fecha de lectura
        for (NotificacionUsuario rel : relaciones) {
            rel.setResuelta(true);
            rel.setFechaLectura(LocalDateTime.now());
        }
        notificacionUsuarioRepository.saveAll(relaciones);

        // 3️⃣ Eliminar únicamente la relación del usuario con la notificación
        notificacionUsuarioRepository.deleteAll(relaciones);

        // ❌ No tocar la tabla Notification, para mantener la notificación del sistema global
    }
}
