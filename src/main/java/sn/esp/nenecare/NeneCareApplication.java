package sn.esp.nenecare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entree de l'application NeneCare.
 * Systeme securise de gestion des dossiers medicaux - Clinique Marose de Ouakam.
 */
@SpringBootApplication
public class NeneCareApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeneCareApplication.class, args);
    }
}
