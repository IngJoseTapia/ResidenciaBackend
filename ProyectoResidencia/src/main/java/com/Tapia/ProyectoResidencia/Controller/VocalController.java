package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.ChangeRoleRequest;
import com.Tapia.ProyectoResidencia.DTO.UsuarioActivoResponse;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Service.UserService;
import com.Tapia.ProyectoResidencia.Utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/vocal")
@PreAuthorize("hasAnyRole('ADMIN', 'VOCAL')")
@RequiredArgsConstructor
public class VocalController {
    private final UserService userService;

    // Obtener todos los usuarios con status ACTIVO (paginado)
    @GetMapping("/activos")
    public ResponseEntity<Page<UsuarioActivoResponse>> listarUsuariosActivos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioActivoResponse> usuariosActivos = userService.listarUsuariosActivos(pageable);
        return ResponseEntity.ok(usuariosActivos);
    }

    // Cambiar rol de un usuario (solo ADMIN)
    @PutMapping("/usuarios/{id}/rol")
    public ResponseEntity<ApiResponse> cambiarRolUsuario(Authentication authentication,
                                                         @PathVariable Long id,
                                                         @RequestBody @Valid ChangeRoleRequest dto,
                                                         HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        userService.cambiarRolUsuarioActivo(id, dto, authentication, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Rol actualizado correctamente ✅", HttpStatus.OK.value()));
    }
}
