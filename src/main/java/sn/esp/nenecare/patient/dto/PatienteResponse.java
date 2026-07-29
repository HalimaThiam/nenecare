package sn.esp.nenecare.patient.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * Vue d'une patiente renvoyee par l'API - donnees DECHIFFREES.
 *
 * Les champs medicaux (groupe sanguin, antecedents) sont laisses a null pour
 * les roles non soignants : la secretaire enregistre les patientes et gere
 * les rendez-vous, elle n'a aucune raison de lire leurs antecedents. Le
 * filtrage est fait au moment de construire cette reponse, donc la donnee ne
 * quitte jamais le serveur - contrairement a un masquage cote navigateur, qui
 * se contourne avec la console du navigateur.
 */
@Data
@Builder
public class PatienteResponse {

    private Long id;
    private String numeroPatiente;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;

    private String telephone;
    private String adresse;

    /** null si l'appelant n'a pas acces au volet medical. */
    private String groupeSanguin;
    private String antecedents;

    private String gynecologueAssigne;

    /** Vrai si l'appelant a recu le volet medical : le frontend s'y adapte. */
    private boolean voletMedicalVisible;

    private Integer nombreDossiersMedicaux;
    private Integer nombreDossiersNeonatals;

    private LocalDateTime creeLe;
}
