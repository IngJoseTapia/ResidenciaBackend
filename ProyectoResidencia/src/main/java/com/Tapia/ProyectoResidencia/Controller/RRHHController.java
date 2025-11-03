package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.*;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.Contrato;
import com.Tapia.ProyectoResidencia.Model.UsuarioContrato;
import com.Tapia.ProyectoResidencia.Service.ContratoService;
import com.Tapia.ProyectoResidencia.Service.UsuarioContratoService;
import com.Tapia.ProyectoResidencia.Service.UsuarioService;
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

import java.util.List;

@RestController
@RequestMapping("/rrhh")
@PreAuthorize("hasAnyRole('ADMIN', 'RRHH')")
@RequiredArgsConstructor
public class RRHHController {

    private final ContratoService contratoService;
    private final UsuarioContratoService  usuarioContratoService;
    private final UsuarioService usuarioService;

    //Crear un nuevo contrato
    @PostMapping("/contrato")
    public ResponseEntity<ApiContratoResponse> crearContrato(@RequestBody @Valid ContratoCreate dto,
                                                     Authentication auth,
                                                     HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Contrato contrato = contratoService.crearContrato(dto, auth, Sitio.WEB, ip);
        // Retornar DTO combinado
        ApiContratoResponse response = new ApiContratoResponse(
                new ApiResponse("Contrato creado correctamente ✅", HttpStatus.OK.value()),
                contrato
        );
        return ResponseEntity.ok(response);
    }

    // Actualizar un contrato existente
    @PutMapping("/contrato/{id}")
    public ResponseEntity<ApiResponse> actualizarContrato(@PathVariable Long id,
                                                                  @RequestBody @Valid ContratoCreate dto,
                                                                  Authentication auth,
                                                                  HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        contratoService.actualizar(auth, id, dto, Sitio.WEB, ip);
        ApiResponse response = new ApiResponse("Contrato actualizado correctamente ✅", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    // Eliminar un contrato
    @DeleteMapping("/contrato/{id}")
    public ResponseEntity<ApiResponse> eliminarContrato(@PathVariable Long id,
                                                        Authentication auth,
                                                        HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        contratoService.eliminar(auth, id, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Contrato eliminado correctamente ✅", HttpStatus.OK.value()));
    }

    // Obtener todos los contratos paginados
    @GetMapping("/contratos/paginado")
    public ResponseEntity<Page<Contrato>> obtenerContratosPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Contrato> contratos = contratoService.obtenerContratosPaginado(pageable);
        return ResponseEntity.ok(contratos);
    }

    // Obtener contrato por ID
    @GetMapping("/contrato/{id}")
    public ResponseEntity<Contrato> obtenerContratoPorId(@PathVariable Long id) {
        Contrato contrato = contratoService.obtenerPorId(id);
        return ResponseEntity.ok(contrato);
    }

    ///////

    //Crear una nueva asignación de contrato a usuario
    @PostMapping("/usuario-contrato/asignar")
    public ResponseEntity<ApiUsuarioContratoResponse> asignarContrato(@RequestBody @Valid UsuarioContratoCreate dto,
                                                                      Authentication auth,
                                                                      HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        UsuarioContrato usuarioContrato = usuarioContratoService.asignarContrato(dto, auth, Sitio.WEB, ip);
        // Retornar DTO combinado
        ApiUsuarioContratoResponse response = new ApiUsuarioContratoResponse(
                new ApiResponse("Contrato asignado al usuario correctamente ✅", HttpStatus.OK.value()),
                usuarioContrato
        );
        return ResponseEntity.ok(response);
    }

    // --- Actualizar estado o observaciones de un contrato ---
    @PutMapping("/usuario-contrato/{id}")
    public ResponseEntity<ApiResponse> actualizarUsuarioContrato(@PathVariable Long id,
                                                          @RequestBody @Valid UsuarioContratoCreate dto,
                                                          Authentication auth,
                                                          HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        usuarioContratoService.actualizarVinculoContrato(id, dto, auth, Sitio.WEB, ip);
        ApiResponse response = new ApiResponse("Contrato de usuario actualizado correctamente ✅", HttpStatus.OK.value());
        return ResponseEntity.ok(response);
    }

    // Obtener todos los vínculos de usuario-contrato paginados
    @GetMapping("/usuario-contratos/paginado")
    public ResponseEntity<Page<UsuarioContratoDTO>> obtenerUsuariosContratosPaginado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioContratoDTO> usuariosContratos = usuarioContratoService.obtenerUsuariosContratosPaginadoDTO(pageable);
        return ResponseEntity.ok(usuariosContratos);
    }

    // Obtener la lista de todos los contratos activos
    @GetMapping("/contratos/activos")
    public ResponseEntity<List<ContratoActivo>> obtenerContratosActivos() {
        List<ContratoActivo> contratos = contratoService.obtenerContratosActivos();
        return ResponseEntity.ok(contratos);
    }

    // Obtener usuarios activos
    @GetMapping("/usuarios/activos")
    public ResponseEntity<List<UsuarioActivo>> obtenerUsuariosActivos() {
        List<UsuarioActivo> usuarios = usuarioService.obtenerUsuariosActivos();
        return ResponseEntity.ok(usuarios);
    }
}
