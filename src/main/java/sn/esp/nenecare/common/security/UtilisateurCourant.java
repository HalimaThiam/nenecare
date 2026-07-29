package sn.esp.nenecare.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import sn.esp.nenecare.auth.service.NeneCareUserDetailsService;

/**
 * Acces a l'identite de l'appelant pour les couches metier et le journal
 * d'audit.
 *
 * L'identite est lue dans le SecurityContext, donc elle vient du JWT valide
 * par JwtAuthFilter - jamais d'un parametre de requete. Un client ne peut pas
 * s'attribuer un role en modifiant le corps de sa requete.
 */
@Component
public class UtilisateurCourant {

    /** Identifiant de l'appelant authentifie. */
    public String username() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        return authentification != null ? authentification.getName() : "anonyme";
    }

    /**
     * Role de l'appelant, sans le prefixe "ROLE_" ajoute pour Spring Security.
     * On renvoie le premier role : dans NeneCare, un compte porte exactement
     * un role clinique.
     */
    public String role() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification == null) {
            return "INCONNU";
        }
        return authentification.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(autorite -> autorite.startsWith(NeneCareUserDetailsService.PREFIXE_ROLE)
                        ? autorite.substring(NeneCareUserDetailsService.PREFIXE_ROLE.length())
                        : autorite)
                .findFirst()
                .orElse("INCONNU");
    }

    public boolean aLeRole(String role) {
        return role().equals(role);
    }

    /**
     * Adresse IP de l'appelant, pour le journal d'audit.
     *
     * X-Forwarded-For est pris en compte car l'application peut tourner
     * derriere un reverse proxy. Cet en-tete etant fourni par le client, il
     * est falsifiable : il sert a l'analyse a posteriori, jamais a une
     * decision d'autorisation.
     */
    public String adresseIp(HttpServletRequest requete) {
        if (requete == null) {
            return null;
        }
        String transmise = requete.getHeader("X-Forwarded-For");
        if (transmise != null && !transmise.isBlank()) {
            return transmise.split(",")[0].trim();
        }
        return requete.getRemoteAddr();
    }
}
