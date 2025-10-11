package com.Tapia.ProyectoResidencia.Model;

import com.Tapia.ProyectoResidencia.Enum.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.*;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; //Id del usuario

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo debe tener un formato válido")
    @Column(unique = true, nullable = false, length = 100)
    private String correo; //Correo del usuario con el que iniciará sesión

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String contrasena; // Contraseña del usuario

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 50, min = 3, message = "El nombre no puede tener más de 50 caracteres ni menos de 3 caracteres")
    private String nombre; //Nombre completo del usuario

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 50, min = 3, message = "El apellido paterno no puede tener más de 50 caracteres ni menos de 3 caracteres")
    private String apellidoPaterno; //Apellido paterno del usuario

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(max = 50, min = 3, message = "El apellido materno no puede tener más de 50 caracteres ni menos de 3 caracteres")
    private String apellidoMaterno; //Apellido materno del usuario

    @NotNull(message = "El estatus es obligatorio")
    @Enumerated(EnumType.STRING)
    private Status status; //Status de la cuenta del usuario, si esta activo o ha sido dado de baja

    @NotNull(message = "El rol es obligatorio")
    @Enumerated(EnumType.STRING)
    private Rol rol; //Rol del usuario

    @NotNull(message = "El género debe ser Masculino, Femenino u Otro")
    @Pattern(regexp = "^(Masculino|Femenino|Otro)$", message = "El género debe ser Masculino, Femenino u Otro")
    private String genero; //Genero del usuario

    @NotNull(message = "El teléfono es obligatorio, debe tener exactamente 10 dígitos")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono debe tener exactamente 10 dígitos")
    private String telefono; //Telefono personal del usuario

    @PastOrPresent(message = "La fecha de registro no puede ser futura")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaRegistro; //Fecha en que se dio de alta la cuenta del usuario

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PasswordResetToken> passwordResetTokens = new ArrayList<>();

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificacionUsuario> notificacionesRecibidas = new ArrayList<>();

    @OneToMany(mappedBy = "emisor")
    private List<Notification> notificacionesEnviadas = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vocalia_id")
    private Vocalia vocalia;
}
