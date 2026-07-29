package sn.esp.nenecare.common.exception;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Repond 401 en JSON quand la requete n'est pas authentifiee.
 *
 * Sans ce bean, Spring Security renvoie une page HTML de connexion ou un
 * en-tete WWW-Authenticate : inexploitable par le frontend (US-22).
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest requete,
                         HttpServletResponse reponse,
                         AuthenticationException exception) throws IOException {

        ApiError erreur = new ApiError(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Non authentifie",
                "Vous devez etre connecte pour acceder a cette ressource."
        );

        reponse.setStatus(HttpStatus.UNAUTHORIZED.value());
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(reponse.getWriter(), erreur);
    }
}
