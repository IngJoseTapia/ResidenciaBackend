package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.ApiVocaliaResponse;
import com.Tapia.ProyectoResidencia.DTO.UsuarioResponse;
import com.Tapia.ProyectoResidencia.DTO.VocaliaAssign;
import com.Tapia.ProyectoResidencia.DTO.VocaliaCreate;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.Usuario;
import com.Tapia.ProyectoResidencia.Model.Vocalia;
import com.Tapia.ProyectoResidencia.Service.VocaliaService;
import com.Tapia.ProyectoResidencia.Utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
        vocaliaService.actualizar(authentication, id, dto, Sitio.WEB,ip);
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
    public ResponseEntity<UsuarioResponse> asignarVocaliaAUsuario(
            @RequestBody @Valid VocaliaAssign dto,
            HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Usuario usuario = vocaliaService.asignarVocaliaAUsuario(dto.usuarioId(), dto.vocaliaId());

        UsuarioResponse response = new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre() + " " + usuario.getApellidoPaterno() + " " + usuario.getApellidoMaterno(),
                usuario.getCorreo(),
                usuario.getRol().name(),
                usuario.getVocalia() != null ? usuario.getVocalia().getNombreCompleto() : null
        );

        return ResponseEntity.ok(response);
    }
}
