package sn.esp.nenecare.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Corps des requetes de creation / modification d'une patiente (US-05, US-06). */
@Data
public class PatienteRequest {

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 80, message = "Le nom ne doit pas depasser 80 caracteres")
    private String nom;

    @NotBlank(message = "Le prenom est obligatoire")
    @Size(max = 80, message = "Le prenom ne doit pas depasser 80 caracteres")
    private String prenom;

    @Past(message = "La date de naissance doit etre dans le passe")
    private LocalDate dateNaissance;

    @Size(max = 40, message = "Le telephone ne doit pas depasser 40 caracteres")
    private String telephone;

    @Size(max = 255, message = "L'adresse ne doit pas depasser 255 caracteres")
    private String adresse;

    @Size(max = 10, message = "Le groupe sanguin ne doit pas depasser 10 caracteres")
    private String groupeSanguin;

    private String antecedents;

    /** Gynecologue referent (username). Determine qui pourra ouvrir les dossiers. */
    @Size(max = 100)
    private String gynecologueAssigne;
}
