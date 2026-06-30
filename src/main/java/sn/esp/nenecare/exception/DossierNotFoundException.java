package sn.esp.nenecare.exception;

import java.util.UUID;

public class DossierNotFoundException extends RuntimeException {

    public DossierNotFoundException(UUID id) {
        super("Dossier médical introuvable : " + id);
    }
}
