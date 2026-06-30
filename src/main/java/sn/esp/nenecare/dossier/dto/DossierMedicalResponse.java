package sn.esp.nenecare.dossier.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DossierMedicalResponse(
    UUID id,
    UUID patientId,
    String nomPatient,
    String antecedentsMedicaux,
    String diagnostics,
    String traitements,
    String allergies,
    String notesConfidentielles,
    String groupeSanguin,
    LocalDateTime dateCreation,
    LocalDateTime dateMiseAJour,
    String creePar,
    String modifiePar,
    Long version
) {}
