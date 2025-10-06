package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.*;
import com.Tapia.ProyectoResidencia.Model.LoginLog;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Repository.LoginLogRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    public void registrarLogsUsuario(Usuario usuario, Evento evento, Resultado resultado, Sitio sitio, String ip, String id) {
        String descripcion;

        switch (evento) {
            case USER_REGISTRADO -> {
                descripcion = "Usuario registrado";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case LOGIN_FALLIDO -> {
                switch (id) {
                    case "1" -> {
                        descripcion = "IP bloqueada temporalmente por múltiples intentos fallidos";
                        registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
                    }
                    case "2" -> {
                        descripcion = "Cuenta bloqueada temporalmente por múltiples intentos fallidos";
                        registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
                    }
                    case "3" -> {
                        descripcion = "Contraseña incorrecta";
                        registrarLog(usuario.getCorreo() != null ? usuario.getCorreo() : null,
                                usuario.getId() != null ? usuario.getId() : null,
                                usuario.getRol() != null ? usuario.getRol() : null,
                                sitio, resultado, descripcion, evento, ip);
                    }
                }
            }
            case LOGIN_EXITOSO -> {
                descripcion = "Inicio de sesión exitoso";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case USER_REGISTRADO_GOOGLE -> {
                descripcion = "Registro de usuario con Google";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case LOGIN_GOOGLE_EXITOSO -> {
                descripcion = "Inicio de sesión con Google exitoso";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case TOKEN_ERROR_INTERNO_VALIDACION ->
                registrarLog(usuario.getCorreo() != null ? usuario.getCorreo() : null,
                        usuario.getId() != null ? usuario.getId() : null,
                        usuario.getRol() != null ? usuario.getRol() : null,
                        sitio, resultado, id, evento, ip);
            case REFRESH_TOKEN_FALLIDO -> {
                descripcion = String.format("""
                                %s. Se requiere iniciar sesión nuevamente.
                                """,
                        id);
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case REFRESH_TOKEN_EXITOSO -> {
                descripcion = "Refresh token exitoso";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case PASSWORD_RESET_SOLICITUD -> {
                if (id.equals("1")) {
                    descripcion = "Cuenta bloqueada temporalmente en solicitud de recuperación";
                    registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
                } else if (id.equals("2")) {
                    descripcion = "Solicitud de cambio de contraseña enviada";
                    registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
                }
            }
            case PASSWORD_RESET_FALLIDO -> {
                if (id.equals("1")) {
                    descripcion = "Cuenta bloqueada temporalmente al verificar token";
                    registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
                } else if (id.equals("2")) {
                    descripcion = "Solicitud de cambio de contraseña enviada";
                    registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
                }
            }
            case PASSWORD_RESET_TOKEN_EXPIRADO -> {
                descripcion = "Token de recuperación expirado";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case PASSWORD_RESET_VERIFICADO -> {
                descripcion = "Token de recuperación verificado";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case PASSWORD_RESET_RECHAZADO -> {
                descripcion = "Contraseña no cumple criterios de seguridad";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
            case PASSWORD_RESET_COMPLETADO -> {
                descripcion = "Contraseña actualizada exitosamente";
                registrarLog(usuario.getCorreo(), usuario.getId(), usuario.getRol(), sitio, resultado, descripcion, evento, ip);
            }
        }
    }

    public void registrarLogsCorreo(String correo, Evento evento, Resultado resultado, Sitio sitio, String ip, String id) {
        String descripcion;
        switch (evento){
            case LOGIN_FALLIDO -> {
                switch (id) {
                    case "0" -> {
                        descripcion = "IP bloqueada temporalmente por múltiples intentos fallidos";
                        registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                    }
                    case "1" -> {
                        descripcion = "Correo no registrado";
                        registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                    }
                    case "2" -> {
                        descripcion = "Contraseña incorrecta";
                        registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                    }
                }
            }
            case TOKEN_ERROR_INTERNO_VALIDACION ->
                registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, id, evento, ip);
            case REFRESH_TOKEN_FALLIDO -> {
                descripcion = "Refresh token inválido";
                registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
            }
            case PASSWORD_RESET_SOLICITUD -> {
                if (id.equals("0")) {
                    descripcion = "IP bloqueada temporalmente en solicitud de recuperación";
                    registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                } else if (id.equals("1")) {
                    descripcion = "Solicitud de recuperación para correo no registrado";
                    registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                }
            }
            case PASSWORD_RESET_FALLIDO -> {
                if (id.equals("0")) {
                    descripcion = "IP bloqueada temporalmente por múltiples intentos fallidos de recuperación";
                    registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                } else if (id.equals("1")) {
                    descripcion = "Solicitud de recuperación para correo no registrado";
                    registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                }
            }
            case PASSWORD_RESET_TOKEN_INVALIDO -> {
                descripcion = "Token de recuperación inválido";
                registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
            }
            case PASSWORD_RESET_ERROR -> {
                if (id.equals("0")) {
                    descripcion = "Error al verificar token";
                    registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                } else if (id.equals("1")) {
                    descripcion = "Error al actualizar contraseña";
                    registrarLog(correo, null, Rol.DESCONOCIDO, sitio, resultado, descripcion, evento, ip);
                }
            }
        }

    }

    private void registrarLog(String correo,
                          Long idUsuario,
                          Rol rol,
                          Sitio sitio,
                          Resultado resultado,
                          String descripcion,
                          Evento tipoEvento,
                          String ip) {
        LoginLog log = new LoginLog();
        log.setCorreo(correo != null ? correo : "Desconocido");
        log.setIdUsuario(idUsuario);
        log.setRol(rol != null ? rol : Rol.DESCONOCIDO);
        log.setSitio(sitio != null ? sitio : Sitio.WEB);
        log.setResultado(resultado);
        log.setDescripcion(descripcion);
        log.setTipoEvento(tipoEvento != null ? tipoEvento : Evento.DESCONOCIDO);
        log.setFechaActividad(Date.from(Instant.now()));
        log.setIp(ip);
        loginLogRepository.save(log);
    }
}
