package sn.esp.nenecare.common.exception;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Corps d'erreur renvoye au client.
 * Message non technique cote utilisateur (US-22) : ne jamais exposer
 * la pile d'exception ni des details d'architecture.
 */
@Data
@AllArgsConstructor
public class ApiError {

    private LocalDateTime horodatage;
    private int statut;
    private String erreur;
    private String message;
}
