package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.ApiContratoResponse;
import com.Tapia.ProyectoResidencia.DTO.ContratoCreate;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.Contrato;
import com.Tapia.ProyectoResidencia.Service.ContratoService;
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
@RequestMapping("/rrhh")
@PreAuthorize("hasAnyRole('ADMIN', 'RRHH')")
@RequiredArgsConstructor
public class RRHHController {

    private final ContratoService contratoService;

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
}
