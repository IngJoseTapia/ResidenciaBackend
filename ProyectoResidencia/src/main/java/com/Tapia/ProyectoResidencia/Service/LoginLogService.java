package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Model.Login;
import com.Tapia.ProyectoResidencia.Repository.LoginRepository;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class LoginLogService {

    private final LoginRepository loginRepository;

    public LoginLogService(LoginRepository loginRepository) {
        this.loginRepository = loginRepository;
    }

    public void registrarLog(Long idUsuario, String correo, Sitio sitio, String resultado, String descripcion, String ip) {
        Login log = new Login();
        log.setIdUsuario(idUsuario);
        log.setCorreo(correo);
        log.setFechaActividad(new Date());
        log.setSitio(sitio);
        log.setResultado(resultado);
        log.setDescripcion(descripcion);
        log.setIp(ip);

        loginRepository.save(log);
    }
}
