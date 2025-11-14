package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Model.*;
import java.util.Set;
import java.util.stream.Collectors;

public record SeccionResponse(
        Long id,
        String numeroSeccion,
        String anio,
        AsignacionZoreAreInfo asignacionZoreAre,
        MunicipioInfo municipio,
        Set<LocalidadInfo> localidades
) {
    public SeccionResponse(Seccion seccion) {
        this(
                seccion.getId(),
                seccion.getNumeroSeccion(),
                seccion.getAnio(),
                new AsignacionZoreAreInfo(seccion.getAsignacionZoreAre()),
                seccion.getLocalidades().isEmpty() ? null :
                        new MunicipioInfo(seccion.getLocalidades().iterator().next().getMunicipio()),
                seccion.getLocalidades().stream().map(LocalidadInfo::new).collect(Collectors.toSet())
        );
    }

    public record AsignacionZoreAreInfo(Long id, String anio, ZoreInfo zore, AreInfo are) {
        public AsignacionZoreAreInfo(AsignacionZoreAre a) {
            this(a.getId(), a.getAnio(), new ZoreInfo(a.getZore()), new AreInfo(a.getAre()));
        }
    }

    public record ZoreInfo(Long id, int numeracion, String anio, UsuarioInfo responsable) {
        public ZoreInfo(Zore z) {
            this(z.getId(), z.getNumeracion(), z.getAnio(), new UsuarioInfo(z.getUsuario()));
        }
    }

    public record AreInfo(Long id, int numeracion, String anio, UsuarioInfo responsable) {
        public AreInfo(Are a) {
            this(a.getId(), a.getNumeracion(), a.getAnio(), new UsuarioInfo(a.getUsuario()));
        }
    }

    public record UsuarioInfo(Long id, String nombreCompleto) {
        public UsuarioInfo(Usuario u) {
            this(u.getId(), String.format("%s %s %s", u.getNombre(), u.getApellidoPaterno(), u.getApellidoMaterno()));
        }
    }

    public record MunicipioInfo(String id, String nombre) {
        public MunicipioInfo(Municipio m) {
            this(m.getId(), m.getNombre());
        }
    }

    public record LocalidadInfo(Long id, String nombre) {
        public LocalidadInfo(Localidad l) {
            this(l.getId(), l.getNombre());
        }
    }
}
