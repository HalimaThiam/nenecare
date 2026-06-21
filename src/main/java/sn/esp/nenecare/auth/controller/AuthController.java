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
            HttpServletRequest http) {
        LoginResponse reponse = authService.login(requete, http.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.ok("Connexion reussie", reponse));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest http) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7), http.getRemoteAddr());
        }
        return ResponseEntity.ok(ApiResponse.ok("Deconnexion reussie", null));
    }
}
