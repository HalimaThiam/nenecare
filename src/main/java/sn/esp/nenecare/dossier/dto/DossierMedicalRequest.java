package sn.esp.nenecare.dossier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record DossierMedicalRequest(

    @NotNull(message = "L'identifiant du patient est obligatoire")
    UUID patientId,

    String antecedentsMedicaux,
    String diagnostics,
    String traitements,
    String allergies,
    String notesConfidentielles,

    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Groupe sanguin invalide (ex: A+, O-, AB+)")
    String groupeSanguin
) {}
