package sn.esp.nenecare.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Corps de la requete POST /api/auth/login (US-01). */
@Data
public class LoginRequest {

    @NotBlank(message = "L'identifiant est obligatoire")
    private String username;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String motDePasse;
}
