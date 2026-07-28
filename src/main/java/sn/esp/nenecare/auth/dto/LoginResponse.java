package sn.esp.nenecare.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Reponse renvoyee apres une connexion reussie : jeton JWT + infos utilisateur. */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String role;
    private long expiresInMs;
}
