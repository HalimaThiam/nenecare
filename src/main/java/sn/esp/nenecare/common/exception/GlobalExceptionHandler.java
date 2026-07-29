package sn.esp.nenecare.common.exception;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Gestion centralisee des erreurs.
 * Renvoie un message clair et non technique (US-22) sans divulguer
 * d'information sensible sur l'architecture.
 *
 * ATTENTION : le handler generique {@code Exception.class} passe AVANT les
 * resolveurs internes de Spring MVC. Sans les handlers specifiques ci-dessous,
 * une simple erreur de validation ou un fichier statique absent serait
 * renvoye en 500.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // Authentification / autorisation
    // -------------------------------------------------------------------------

    /** Identifiants refuses (US-01) : 401, jamais 400. */
    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ApiError> handleIdentifiantsInvalides(
            IdentifiantsInvalidesException ex) {
        return construire(HttpStatus.UNAUTHORIZED, "Authentification refusee", ex.getMessage());
    }

    /** Compte bloque apres trop d'echecs (US-04). */
    @ExceptionHandler(CompteBloqueException.class)
    public ResponseEntity<ApiError> handleCompteBloque(CompteBloqueException ex) {
        return construire(HttpStatus.LOCKED, "Acces temporairement bloque", ex.getMessage());
    }

    /** Exception d'authentification levee hors de la chaine de filtres. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex) {
        return construire(HttpStatus.UNAUTHORIZED, "Non authentifie",
                "Vous devez etre connecte pour acceder a cette ressource.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return construire(HttpStatus.FORBIDDEN, "Acces refuse",
                "Vous n'avez pas les droits necessaires pour cette action.");
    }

    // -------------------------------------------------------------------------
    // Requetes invalides
    // -------------------------------------------------------------------------

    /** Echec de @Valid : 400 avec le detail des champs fautifs. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return construire(HttpStatus.BAD_REQUEST, "Requete invalide",
                details.isBlank() ? "Requete invalide." : details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return construire(HttpStatus.BAD_REQUEST, "Requete invalide", ex.getMessage());
    }

    /** Ressource metier demandee mais inexistante (patiente, dossier). */
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ApiError> handleRessourceIntrouvable(RessourceIntrouvableException ex) {
        return construire(HttpStatus.NOT_FOUND, "Introuvable", ex.getMessage());
    }

    /** Ressource statique ou endpoint inexistant : 404, pas 500. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleIntrouvable(NoResourceFoundException ex) {
        return construire(HttpStatus.NOT_FOUND, "Introuvable",
                "La ressource demandee n'existe pas.");
    }

    // -------------------------------------------------------------------------
    // Filet de securite
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return construire(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur est survenue. Contactez l'administrateur si le probleme persiste.");
    }

    private ResponseEntity<ApiError> construire(HttpStatus statut, String erreur, String message) {
        return ResponseEntity.status(statut).body(
                new ApiError(LocalDateTime.now(), statut.value(), erreur, message));
    }
}
