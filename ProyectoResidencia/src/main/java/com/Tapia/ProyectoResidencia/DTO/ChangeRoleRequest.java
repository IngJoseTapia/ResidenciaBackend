package com.Tapia.ProyectoResidencia.DTO;

import com.Tapia.ProyectoResidencia.Enum.Rol;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Rol rol) {}
