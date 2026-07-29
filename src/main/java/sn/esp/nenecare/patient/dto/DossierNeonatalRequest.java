package sn.esp.nenecare.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Corps des requetes de creation / modification d'un dossier neonatal (US-10 a US-13). */
@Data
public class DossierNeonatalRequest {

    /** Mere du nouveau-ne : obligatoire, c'est la liaison mere-enfant (OS-06). */
    @NotNull(message = "La mere est obligatoire : un dossier neonatal ne peut exister seul")
    private Long mereId;

    @NotBlank(message = "Le nom du nouveau-ne est obligatoire")
    @Size(max = 120)
    private String nomBebe;

    @Pattern(regexp = "[MF]", message = "Le sexe doit etre M ou F")
    private String sexe;

    @NotNull(message = "La date de naissance est obligatoire")
    private LocalDate dateNaissance;

    @Min(value = 200,  message = "Le poids doit etre superieur a 200 g")
    @Max(value = 8000, message = "Le poids doit etre inferieur a 8000 g")
    private Integer poidsGrammes;

    @Min(value = 15) @Max(value = 70)
    private Integer tailleCm;

    @Min(value = 0) @Max(value = 10)
    private Integer scoreApgar;

    /** Chiffre avant enregistrement (OS-03). */
    private String diagnostic;

    /** Chiffre avant enregistrement (OS-03). */
    private String observations;
}
