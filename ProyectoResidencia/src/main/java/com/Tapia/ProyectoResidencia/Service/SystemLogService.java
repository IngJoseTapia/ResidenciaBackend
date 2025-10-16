package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Model.SystemLog;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class SystemLogService {
    public final SystemLogRepository systemLogRepository;

    public void registrarLogUsuario(Usuario usuario, Evento evento, Resultado resultado, Sitio sitio, String ip, String id){
        String descripcion;

        switch (evento) {
            case UPDATE_INFO_USUARIO_EXITOSO -> {
                descripcion = "Actualización de datos personales exitosa";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case UPDATE_INFO_USUARIO_FALLIDO ->
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, id, ip);
            case PASSWORD_CHANGE_FALLIDO -> {
                switch (id) {
                    case "0" -> {
                        descripcion = "IP bloqueada temporalmente por múltiples intentos fallidos";
                        registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                    }
                    case "1" -> {
                        descripcion = "Cuenta bloqueada temporalmente por múltiples intentos fallidos";
                        registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                    }
                    case "2" -> {
                        descripcion = "Contraseña actual nula o en blanco";
                        registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                    }
                    case "3" -> {
                        descripcion = "No se requiere contraseña actual para generar contraseña";
                        registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                    }
                    case "4" -> {
                        descripcion = "Nueva contraseña o confirmar contraseña nula o en blanco";
                        registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                    }
                    case "5" -> {
                        descripcion = "Las contraseñas no coinciden";
                        registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                    }
                    case "6" -> {
                        descripcion = "La nueva contraseña no cumple los criterios de seguridad";
                        registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                    }
                }
            }
            case PASSWORD_CHANGE_EXCESIVOS -> {
                descripcion = "Cuenta bloqueada temporalmente por múltiples cambios de contraseña";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case PASSWORD_CHANGE_RECHAZADO -> {
                descripcion = "Contraseña actual incorrecta";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case PASSWORD_CHANGE_ERROR -> {
                if (id.equals("1")) {
                    descripcion = "Error de validación al cambiar la contraseña";
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                } else if (id.equals("2")) {
                    descripcion = "Ocurrió un error inesperado al intentar cambiar la contraseña del usuario";
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                }
            }
            case PASSWORD_CHANGE_EXITOSO -> {
                descripcion = "Contraseña actualizada exitosamente";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case PASSWORD_CHANGE_PRIMERA_VEZ -> {
                descripcion = "Contraseña generada exitosamente";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case VOCALIA_REGISTER_EXITOSO -> {
                descripcion = "Vocalía registrada exitosamente";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case VOCALIA_REGISTER_FALLIDO -> {
                descripcion = "Ya existe una vocalía con la misma abreviatura";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case VOCALIA_REGISTER_ERROR -> {
                if (id.equals("1")){
                    descripcion = "Ocurrió un error interno en el sistema al registrar la vocalía";
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                } else {
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, id, ip);
                }
            }
            case VOCALIA_UPDATE_EXITOSO -> {
                descripcion = "Vocalía actualizada exitosamente";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case VOCALIA_UPDATE_FALLIDO -> {
                descripcion = "Ya existe un registro de vocalía con la misma abreviatura";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case VOCALIA_UPDATE_ERROR -> {
                if (id.equals("1")){
                    descripcion = "Ocurrió un error interno en el sistema al actualizar la vocalía";
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                } else {
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, id, ip);
                }
            }
            case VOCALIA_DELETE_EXITOSO -> {
                descripcion = "Vocalía eliminada exitosamente";
                registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
            }
            case VOCALIA_DELETE_ERROR -> {
                if (id.equals("1")){
                    descripcion = "Ocurrió un error interno en el sistema al eliminar la vocalía";
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, descripcion, ip);
                } else {
                    registrarLog(usuario.getId(), usuario.getCorreo(), usuario.getRol(), sitio, evento, resultado, id, ip);
                }
            }
        }
    }

    private void registrarLog(Long idUsuario, String correo, Rol rol, Sitio sitio, Evento evento, Resultado resultado, String descripcion, String ip) {
        SystemLog  systemLog = new SystemLog();
        systemLog.setIdUsuario(idUsuario);
        systemLog.setCorreo(correo);
        systemLog.setRol(rol);
        systemLog.setFechaActividad(Date.from(Instant.now()));
        systemLog.setSitio(sitio);
        systemLog.setTipoEvento(evento);
        systemLog.setResultado(resultado);
        systemLog.setDescripcion(descripcion);
        systemLog.setIp(ip);
        systemLogRepository.save(systemLog);
    }
}
