package sn.esp.nenecare.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Corps des requetes de creation / modification d'un dossier medical (US-07, US-08). */
@Data
public class DossierMedicalRequest {

    @NotNull(message = "La patiente concernee est obligatoire")
    private Long patienteId;

    @NotNull(message = "La date de consultation est obligatoire")
    private LocalDate dateConsultation;

    @Size(max = 200, message = "Le motif ne doit pas depasser 200 caracteres")
    private String motif;

    /** Chiffre avant enregistrement (OS-03). */
    private String diagnostic;

    /** Chiffre avant enregistrement (OS-03). */
    private String traitement;

    /** Chiffre avant enregistrement (OS-03). */
    private String observations;
}
