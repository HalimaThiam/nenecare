package sn.esp.nenecare.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Reponse renvoyee apres une connexion reussie : jeton JWT + infos utilisateur.
 *
 * Le role est renvoye pour que le frontend puisse adapter l'affichage. Il ne
 * constitue PAS une autorisation : chaque appel d'API est reverifie cote
 * serveur a partir du jeton.
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String username;
    private String nomComplet;
    private String role;
    private long expiresInMs;
}
