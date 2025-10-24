package com.Tapia.ProyectoResidencia.Service;

import com.Tapia.ProyectoResidencia.Enum.Evento;
import com.Tapia.ProyectoResidencia.Enum.Resultado;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Model.SystemLog;
import com.Tapia.ProyectoResidencia.Repository.SystemLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class SystemLogTransactionalService {

    private final SystemLogRepository systemLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarLog(Long idUsuario, String correo, Rol rol, Sitio sitio, Evento evento, Resultado resultado, String descripcion, String ip) {
        SystemLog systemLog = new SystemLog();
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
