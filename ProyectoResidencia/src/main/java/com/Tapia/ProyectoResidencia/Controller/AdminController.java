package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.*;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.Vocalia;
import com.Tapia.ProyectoResidencia.Service.UserService;
import com.Tapia.ProyectoResidencia.Service.VocaliaService;
import com.Tapia.ProyectoResidencia.Utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {
    private final VocaliaService vocaliaService;
    private final UserService userService;

    // Listar todas las vocalías
    @GetMapping("/vocalia")
    public ResponseEntity<List<Vocalia>> listarTodas() {
        return ResponseEntity.ok(vocaliaService.listarTodas());
    }

    // Crear nueva vocalía (solo ADMIN)
    @PostMapping("/vocalia")
    public ResponseEntity<ApiVocaliaResponse> crear(Authentication authentication,
                                                    @RequestBody @Valid VocaliaCreate dto,
                                                    HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Vocalia nueva = vocaliaService.crear(authentication, dto, Sitio.WEB, ip);
        // Retornar DTO combinado
        ApiVocaliaResponse response = new ApiVocaliaResponse(
                new ApiResponse("Vocalía creada correctamente ✅", HttpStatus.OK.value()),
                nueva
        );
        return ResponseEntity.ok(response);
    }

    // Actualizar vocalía (solo ADMIN)
    @PutMapping("/vocalia/{id}")
    public ResponseEntity<ApiResponse> actualizar(Authentication authentication,
                                                  @PathVariable Long id,
                                                  @RequestBody @Valid VocaliaCreate dto,
                                                  HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        vocaliaService.actualizar(authentication, id, dto, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Vocalía actualizada correctamente ✅", HttpStatus.OK.value()));
    }

    // Eliminar vocalía (solo ADMIN)
    @DeleteMapping("/vocalia/{id}")
    public ResponseEntity<ApiResponse> eliminar(Authentication authentication,
                                                @PathVariable Long id,
                                                HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        vocaliaService.eliminar(authentication, id, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Vocalía eliminada correctamente ✅", HttpStatus.OK.value()));
    }

    // Asignar vocalía a un usuario (solo ADMIN)
    @PostMapping("/asignar-vocalia")
    public ResponseEntity<ApiResponse> asignarVocaliaAUsuario(Authentication authentication,
                                                              @RequestBody @Valid VocaliaAssign dto,
                                                              HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        vocaliaService.asignarVocaliaAUsuario(authentication, dto, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Se vinculó la vocalía al usuario correctamente ✅", HttpStatus.OK.value()));
    }

    // Obtener todos los usuarios con status PENDIENTE (solo ADMIN)
    @GetMapping("/pendientes")
    public ResponseEntity<Page<UsuarioPendienteAsignacion>> listarUsuariosPendientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioPendienteAsignacion> usuariosPendientes = userService.listarUsuariosPendientes(pageable);
        return ResponseEntity.ok(usuariosPendientes);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<Page<UsuarioResumen>> listarTodosUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioResumen> usuarios = userService.listarTodosUsuarios(pageable);

        return ResponseEntity.ok(usuarios);
    }

    // Elimina el registro de usuarios con status PENDIENTE
    @DeleteMapping("/eliminar-pendiente/{id}")
    public ResponseEntity<ApiResponse> eliminarUsuarioPendiente(@PathVariable Long id,
                                                                Authentication auth,
                                                                HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        userService.eliminarUsuarioPendiente(id, auth, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Se eliminó el usuario correctamente ✅", HttpStatus.OK.value()));
    }

    // Elimina el registro de usuarios con status INACTIVO y conserva datos sensibles
    @DeleteMapping("/eliminar/{id}")
    public ResponseEntity<ApiResponse> eliminarUsuario(@PathVariable Long id,
                                                       Authentication auth,
                                                       HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        userService.eliminarUsuario(id, auth, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Se eliminó el usuario correctamente ✅", HttpStatus.OK.value()));
    }
}
