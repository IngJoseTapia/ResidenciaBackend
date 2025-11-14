package com.Tapia.ProyectoResidencia.DTO;

import java.util.Set;

public record SeccionCreate(
        String numeroSeccion,
        String anio,
        Long asignacionZoreAreId,
        Set<Long> localidadesIds
) {}
