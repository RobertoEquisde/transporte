// LoginRequest.java (actualización de tu AuthRequest)
package com.adavec.transporte.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username o email es obligatorio")
    private String usernameOrEmail;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
