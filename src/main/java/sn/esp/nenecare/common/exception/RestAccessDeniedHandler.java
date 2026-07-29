package sn.esp.nenecare.common.exception;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Repond 403 en JSON quand l'utilisateur est authentifie mais que son role
 * ne l'autorise pas (RBAC, OS-05). Message non technique (US-22).
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest requete,
                       HttpServletResponse reponse,
                       AccessDeniedException exception) throws IOException {

        ApiError erreur = new ApiError(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "Acces refuse",
                "Vous n'avez pas les droits necessaires pour cette action."
        );

        reponse.setStatus(HttpStatus.FORBIDDEN.value());
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(reponse.getWriter(), erreur);
    }
}
