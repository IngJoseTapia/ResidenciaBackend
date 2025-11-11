package com.Tapia.ProyectoResidencia.Controller;

import com.Tapia.ProyectoResidencia.DTO.*;
import com.Tapia.ProyectoResidencia.Enum.Rol;
import com.Tapia.ProyectoResidencia.Enum.Sitio;
import com.Tapia.ProyectoResidencia.Exception.ApiResponse;
import com.Tapia.ProyectoResidencia.Model.*;
import com.Tapia.ProyectoResidencia.Service.*;
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
    private final LoginLogService loginLogService;
    private final SystemLogService systemLogService;
    private final EmailLogService emailLogService;
    private final MunicipioService municipioService;
    private final ZoreService zoreService;
    private final UsuarioService usuarioService;
    private final AreService areService;
    private final AsignacionZoreAreService asignacionZoreAreService;
    private final LocalidadService localidadService;

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
    public ResponseEntity<Page<UsuarioPendienteAsignacion>> listarUsuariosPendientes(@RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UsuarioPendienteAsignacion> usuariosPendientes = userService.listarUsuariosPendientes(pageable);
        return ResponseEntity.ok(usuariosPendientes);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<Page<UsuarioResumen>> listarTodosUsuarios(@RequestParam(defaultValue = "0") int page,
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

    //Listar todos los logs del login
    @GetMapping("/logs/login")
    public ResponseEntity<Page<LoginLog>> listarLogsLogin(@RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "50") int size) {
        int maxSize = Math.min(size, 100); // Límite de seguridad
        Pageable pageable = PageRequest.of(page, maxSize);
        Page<LoginLog> logs = loginLogService.listarLogsLogin(pageable);
        return ResponseEntity.ok(logs);
    }

    // ✅ Listar todos los logs del sistema
    @GetMapping("/logs/sistema")
    public ResponseEntity<Page<SystemLog>> listarLogsSistema(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "50") int size) {
        int maxSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, maxSize);
        Page<SystemLog> logs = systemLogService.listarLogsSistema(pageable);
        return ResponseEntity.ok(logs);
    }

    // ✅ Listar todos los logs de correos enviados
    @GetMapping("/logs/correos")
    public ResponseEntity<Page<EmailLog>> listarLogsCorreos(@RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "50") int size) {
        int maxSize = Math.min(size, 100); // Límite de seguridad
        Pageable pageable = PageRequest.of(page, maxSize);
        Page<EmailLog> logs = emailLogService.listarLogsCorreo(pageable);
        return ResponseEntity.ok(logs);
    }

    // Cambiar el correo del usuario
    @PutMapping("/usuario/{id}/correo")
    public ResponseEntity<ApiResponse> actualizarCorreoUsuario(@PathVariable Long id,
                                                               @RequestBody @Valid UpdateUserEmailRequest request,
                                                               Authentication auth,
                                                               HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        userService.actualizarCorreoUsuario(id, request, auth, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Correo actualizado correctamente ✅", HttpStatus.OK.value()));
    }

    // ✅ Actualizar la contraseña de un usuario (solo ADMIN)
    @PutMapping("/usuario/{id}/password")
    public ResponseEntity<ApiResponse> actualizarContrasenaUsuario(@PathVariable Long id,
                                                                   @RequestBody @Valid UpdateUserPasswordRequest request,
                                                                   Authentication auth,
                                                                   HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        userService.actualizarContrasenaUsuario(id, request, auth, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Contraseña actualizada correctamente ✅", HttpStatus.OK.value()));
    }

    @PutMapping("/usuario/{id}/status")
    public ResponseEntity<ApiResponse> actualizarStatusUsuario(@PathVariable Long id,
                                                               @RequestBody @Valid UpdateUserStatusRequest request,
                                                               Authentication auth,
                                                               HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        userService.actualizarStatusUsuario(id, request, auth, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Status de usuario actualizado correctamente ✅", HttpStatus.OK.value()));
    }

    // Listar todos los municipios
    @GetMapping("/municipio")
    public ResponseEntity<List<Municipio>> listarMunicipios() {
        return ResponseEntity.ok(municipioService.listarTodos());
    }

    // Crear municipio
    @PostMapping("/municipio")
    public ResponseEntity<ApiMunicipioResponse> crearMunicipio(Authentication authentication,
                                                      @RequestBody @Valid Municipio dto,
                                                      HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Municipio municipio = municipioService.crear(authentication, dto, Sitio.WEB, ip);
        ApiMunicipioResponse response = new ApiMunicipioResponse(
                new ApiResponse("Municipio creado correctamente ✅", HttpStatus.OK.value()),
                municipio
        );
        return ResponseEntity.ok(response);
    }

    // ✅ Actualizar municipio (permitiendo cambio de ID)
    @PutMapping("/municipio/{idActual}")
    public ResponseEntity<ApiMunicipioResponse> actualizarMunicipio(Authentication authentication,
                                                                    @PathVariable("idActual") String idActual,
                                                                    @RequestBody @Valid Municipio dto,
                                                                    HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Municipio municipioActualizado = municipioService.actualizar(authentication, idActual, dto, Sitio.WEB, ip);

        ApiMunicipioResponse response = new ApiMunicipioResponse(
                new ApiResponse("Municipio actualizado correctamente ✅", HttpStatus.OK.value()),
                municipioActualizado
        );

        return ResponseEntity.ok(response);
    }

    // Eliminar municipio
    @DeleteMapping("/municipio/{id}")
    public ResponseEntity<ApiResponse> eliminarMunicipio(Authentication authentication,
                                                         @PathVariable String id,
                                                         HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        municipioService.eliminar(authentication, id, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Municipio eliminado correctamente ✅", HttpStatus.OK.value()));
    }

    // ✅ Listar Zores con paginación
    @GetMapping("/zore")
    public ResponseEntity<Page<ZoreResponse>> listarZoresPaginadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int maxSize = Math.min(size, 100); // Límite de seguridad
        Pageable pageable = PageRequest.of(page, maxSize);
        Page<ZoreResponse> zores = zoreService.listarPaginadas(pageable);

        return ResponseEntity.ok(zores);
    }

    // Crear una nueva Zore
    @PostMapping("/zore")
    public ResponseEntity<ApiZoreResponse> crearZore(Authentication authentication,
                                                     @RequestBody @Valid ZoreCreate dto,
                                                     HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Zore nueva = zoreService.crear(authentication, dto, Sitio.WEB, ip);

        ApiZoreResponse response = new ApiZoreResponse(
                new ApiResponse("Zore creada correctamente ✅", HttpStatus.OK.value()),
                new ZoreResponse(nueva)
        );
        return ResponseEntity.ok(response);
    }

    // Actualizar una Zore existente
    @PutMapping("/zore/{id}")
    public ResponseEntity<ApiZoreResponse> actualizarZore(Authentication authentication,
                                                      @PathVariable Long id,
                                                      @RequestBody @Valid ZoreCreate dto,
                                                      HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Zore actualizada = zoreService.actualizar(authentication, id, dto, Sitio.WEB, ip);
        ApiZoreResponse response = new ApiZoreResponse(
                new ApiResponse("Zore actualizada correctamente ✅", HttpStatus.OK.value()),
                new ZoreResponse(actualizada)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/usuarios/rol-se")
    public ResponseEntity<List<UsuarioActivo>> listarUsuariosConRolSE() {
        List<UsuarioActivo> usuariosSE = usuarioService.obtenerUsuariosPorRol(Rol.SE);
        return ResponseEntity.ok(usuariosSE);
    }

    // ✅ Listar ARE con paginación
    @GetMapping("/are")
    public ResponseEntity<Page<AreResponse>> listarAresPaginadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int maxSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, maxSize);
        Page<AreResponse> ares = areService.listarPaginadas(pageable);

        return ResponseEntity.ok(ares);
    }

    // ✅ Crear una nueva ARE
    @PostMapping("/are")
    public ResponseEntity<ApiAreResponse> crearAre(Authentication authentication,
                                                   @RequestBody @Valid AreCreate dto,
                                                   HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Are nueva = areService.crear(authentication, dto, Sitio.WEB, ip);

        ApiAreResponse response = new ApiAreResponse(
                new ApiResponse("Are creada correctamente ✅", HttpStatus.OK.value()),
                new AreResponse(nueva)
        );
        return ResponseEntity.ok(response);
    }

    // ✅ Actualizar una ARE existente
    @PutMapping("/are/{id}")
    public ResponseEntity<ApiAreResponse> actualizarAre(Authentication authentication,
                                                        @PathVariable Long id,
                                                        @RequestBody @Valid AreCreate dto,
                                                        HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        Are actualizada = areService.actualizar(authentication, id, dto, Sitio.WEB, ip);
        ApiAreResponse response = new ApiAreResponse(
                new ApiResponse("Are actualizada correctamente ✅", HttpStatus.OK.value()),
                new AreResponse(actualizada)
        );
        return ResponseEntity.ok(response);
    }

    // ✅ Listar usuarios con rol CAE
    @GetMapping("/usuarios/rol-cae")
    public ResponseEntity<List<UsuarioActivo>> listarUsuariosConRolCAE() {
        List<UsuarioActivo> usuariosCAE = usuarioService.obtenerUsuariosPorRol(Rol.CAE);
        return ResponseEntity.ok(usuariosCAE);
    }

    // ✅ Listar todas las asignaciones (paginado)
    @GetMapping("/asignacion-zore-are")
    public ResponseEntity<Page<AsignacionZoreAreResponse>> listarAsignaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<AsignacionZoreAreResponse> asignaciones = asignacionZoreAreService.listarPaginadas(pageable);
        return ResponseEntity.ok(asignaciones);
    }

    // ✅ Crear nueva asignación ZORE–ARE
    @PostMapping("/asignacion-zore-are")
    public ResponseEntity<ApiAsignacionZoreAreResponse> crearAsignacion(
            Authentication authentication,
            @RequestBody @Valid AsignacionZoreAreCreate dto,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extractClientIp(httpRequest);
        AsignacionZoreAre nueva = asignacionZoreAreService.crear(authentication, dto, Sitio.WEB, ip);

        ApiAsignacionZoreAreResponse response = new ApiAsignacionZoreAreResponse(
                new ApiResponse("Asignación ZORE–ARE creada correctamente ✅", HttpStatus.OK.value()),
                new AsignacionZoreAreResponse(nueva)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    // ✅ Actualizar asignación existente
    @PutMapping("/asignacion-zore-are/{id}")
    public ResponseEntity<ApiAsignacionZoreAreResponse> actualizarAsignacion(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody @Valid AsignacionZoreAreCreate dto,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extractClientIp(httpRequest);
        AsignacionZoreAre actualizada = asignacionZoreAreService.actualizar(authentication, id, dto, Sitio.WEB, ip);

        ApiAsignacionZoreAreResponse response = new ApiAsignacionZoreAreResponse(
                new ApiResponse("Asignación ZORE–ARE actualizada correctamente ✅", HttpStatus.OK.value()),
                new AsignacionZoreAreResponse(actualizada)
        );

        return ResponseEntity.ok(response);
    }

    // 🔹 Listar años disponibles en ZORE (solo un registro por año)
    @GetMapping("/zore/anos")
    public ResponseEntity<List<IdAnioDTO>> listarAniosZore() {
        List<IdAnioDTO> anos = zoreService.obtenerAniosUnicos();
        return ResponseEntity.ok(anos);
    }

    // 🔹 Listar ZORE por año seleccionado
    @GetMapping("/zore/por-anio")
    public ResponseEntity<List<ZoreResponse>> listarZoresPorAnio(@RequestParam String anio) {
        List<ZoreResponse> zores = zoreService.listarPorAnio(anio);
        return ResponseEntity.ok(zores);
    }

    // 🔹 Listar ARE disponibles por año (sin asignar)
    // ahora acepta optional includeId para edición
    @GetMapping("/are/por-anio")
    public ResponseEntity<List<AreResponse>> listarAresPorAnio(
            @RequestParam String anio,
            @RequestParam(required = false) Long includeId) {

        List<AreResponse> ares = areService.listarPorAnioSinAsignacion(anio, includeId);
        return ResponseEntity.ok(ares);
    }

    // ✅ Listar todas las localidades
    @GetMapping("/localidad")
    public ResponseEntity<List<Localidad>> listarLocalidades() {
        return ResponseEntity.ok(localidadService.listarTodas());
    }

    // ✅ Crear nueva localidad
    @PostMapping("/localidad")
    public ResponseEntity<ApiLocalidadResponse> crearLocalidad(
            Authentication authentication,
            @RequestBody @Valid LocalidadCreate dto,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extractClientIp(httpRequest);
        Localidad nueva = localidadService.crear(authentication, dto, Sitio.WEB, ip);

        ApiLocalidadResponse response = new ApiLocalidadResponse(
                new ApiResponse("Localidad creada correctamente ✅", HttpStatus.OK.value()),
                new LocalidadResponse(nueva)
        );
        return ResponseEntity.ok(response);
    }

    // ✅ Actualizar localidad existente
    @PutMapping("/localidad/{id}")
    public ResponseEntity<ApiLocalidadResponse> actualizarLocalidad(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody @Valid LocalidadCreate dto,
            HttpServletRequest httpRequest) {

        String ip = IpUtils.extractClientIp(httpRequest);
        Localidad actualizada = localidadService.actualizar(authentication, id, dto, Sitio.WEB, ip);

        ApiLocalidadResponse response = new ApiLocalidadResponse(
                new ApiResponse("Localidad actualizada correctamente ✅", HttpStatus.OK.value()),
                new LocalidadResponse(actualizada)
        );

        return ResponseEntity.ok(response);
    }

    // ✅ Eliminar localidad
    @DeleteMapping("/localidad/{id}")
    public ResponseEntity<ApiResponse> eliminarLocalidad(Authentication authentication,
                                                         @PathVariable Long id,
                                                         HttpServletRequest httpRequest) {
        String ip = IpUtils.extractClientIp(httpRequest);
        localidadService.eliminar(authentication, id, Sitio.WEB, ip);
        return ResponseEntity.ok(new ApiResponse("Localidad eliminada correctamente ✅", HttpStatus.OK.value()));
    }

    // ✅ Listar localidades con paginación
    @GetMapping("/localidad/paginadas")
    public ResponseEntity<Page<LocalidadResponse>> listarLocalidadesPaginadas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int maxSize = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, maxSize);
        Page<LocalidadResponse> localidades = localidadService.listarPaginadas(pageable);

        return ResponseEntity.ok(localidades);
    }

}
