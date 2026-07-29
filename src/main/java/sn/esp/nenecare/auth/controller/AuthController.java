package sn.esp.nenecare.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.auth.dto.LoginRequest;
import sn.esp.nenecare.auth.dto.LoginResponse;
import sn.esp.nenecare.auth.service.AuthService;
import sn.esp.nenecare.common.dto.ApiResponse;

/**
 * Points d'entree d'authentification.
 *
 * Proprietaire : Elimane (auth).
 *  - POST /api/auth/login  : US-01 (connexion + JWT)
 *  - POST /api/auth/logout : US-03 (revocation de session)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest requete,
            HttpServletRequest httpRequete) {

        LoginResponse reponse = authService.login(requete, adresseIp(httpRequete));
        return ResponseEntity.ok(ApiResponse.ok("Connexion reussie", reponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequete) {

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7).trim(), adresseIp(httpRequete));
        }
        return ResponseEntity.ok(ApiResponse.ok("Deconnexion reussie", null));
    }

    /**
     * Adresse IP de l'appelant, pour le journal d'audit.
     *
     * X-Forwarded-For est pris en compte car l'application peut tourner
     * derriere un reverse proxy. NOTE : cet en-tete est fourni par le client
     * et donc falsifiable ; il ne doit servir qu'a l'analyse a posteriori,
     * jamais a une decision d'autorisation.
     */
    private String adresseIp(HttpServletRequest requete) {
        String transmise = requete.getHeader("X-Forwarded-For");
        if (transmise != null && !transmise.isBlank()) {
            return transmise.split(",")[0].trim();
        }
        return requete.getRemoteAddr();
    }
}
