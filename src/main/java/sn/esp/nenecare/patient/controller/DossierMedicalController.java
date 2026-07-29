package sn.esp.nenecare.patient.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.common.dto.ApiResponse;
import sn.esp.nenecare.common.security.UtilisateurCourant;
import sn.esp.nenecare.patient.dto.DossierMedicalRequest;
import sn.esp.nenecare.patient.dto.DossierMedicalResponse;
import sn.esp.nenecare.patient.service.DossierMedicalService;

/**
 * API des dossiers medicaux (suivi de grossesse) - US-07, US-08, US-09.
 *
 * Proprietaire : Amadou (patient).
 *
 * RBAC applique ici (OS-05) :
 *  - REDIGER / MODIFIER : GYNECOLOGUE, SAGE_FEMME. Ce sont les seuls a poser
 *    un diagnostic obstetrical.
 *  - CONSULTER : les redacteurs + INFIRMIER (execution des soins) + ADMIN.
 *  - ARCHIVER : ADMIN uniquement (US-09), acte de gestion et non de soin.
 *  - SECRETAIRE : aucun acces. Elle enregistre les patientes, pas leurs
 *    diagnostics - c'est le cloisonnement demande par la charte.
 */
@RestController
@RequestMapping("/api/dossiers")
@RequiredArgsConstructor
public class DossierMedicalController {

    private static final String ROLES_LECTURE =
            "hasAnyRole('GYNECOLOGUE','SAGE_FEMME','INFIRMIER','ADMIN')";
    private static final String ROLES_REDACTION =
            "hasAnyRole('GYNECOLOGUE','SAGE_FEMME')";

    private final DossierMedicalService dossierMedicalService;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping
    @PreAuthorize(ROLES_LECTURE)
    public ResponseEntity<ApiResponse<List<DossierMedicalResponse>>> lister(
            HttpServletRequest requete) {
        return ResponseEntity.ok(ApiResponse.ok(
                dossierMedicalService.lister(utilisateurCourant.adresseIp(requete))));
    }

    /**
     * Consultation d'un dossier.
     *
     * @param motifUrgence acces derogatoire a une patiente non assignee (US-14).
     *                     Autorise, mais trace comme ACCES_URGENCE dans le journal.
     */
    @GetMapping("/{id}")
    @PreAuthorize(ROLES_LECTURE)
    public ResponseEntity<ApiResponse<DossierMedicalResponse>> consulter(
            @PathVariable Long id,
            @RequestParam(required = false) String motifUrgence,
            HttpServletRequest requete) {

        return ResponseEntity.ok(ApiResponse.ok(dossierMedicalService.consulter(
                id, motifUrgence, utilisateurCourant.adresseIp(requete))));
    }

    /** Historique complet d'une patiente. */
    @GetMapping("/patiente/{patienteId}")
    @PreAuthorize(ROLES_LECTURE)
    public ResponseEntity<ApiResponse<List<DossierMedicalResponse>>> listerParPatiente(
            @PathVariable Long patienteId,
            HttpServletRequest requete) {

        return ResponseEntity.ok(ApiResponse.ok(dossierMedicalService.listerParPatiente(
                patienteId, utilisateurCourant.adresseIp(requete))));
    }

    @PostMapping
    @PreAuthorize(ROLES_REDACTION)
    public ResponseEntity<ApiResponse<DossierMedicalResponse>> creer(
            @Valid @RequestBody DossierMedicalRequest corps,
            HttpServletRequest requete) {

        DossierMedicalResponse cree = dossierMedicalService.creer(
                corps, utilisateurCourant.adresseIp(requete));
        return ResponseEntity.ok(ApiResponse.ok("Dossier cree et chiffre", cree));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ROLES_REDACTION)
    public ResponseEntity<ApiResponse<DossierMedicalResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody DossierMedicalRequest corps,
            HttpServletRequest requete) {

        DossierMedicalResponse modifie = dossierMedicalService.modifier(
                id, corps, utilisateurCourant.adresseIp(requete));
        return ResponseEntity.ok(ApiResponse.ok("Dossier mis a jour", modifie));
    }

    /** US-09 : fermeture du dossier. Il reste consultable et signe. */
    @PutMapping("/{id}/archiver")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DossierMedicalResponse>> archiver(
            @PathVariable Long id,
            HttpServletRequest requete) {

        DossierMedicalResponse archive = dossierMedicalService.archiver(
                id, utilisateurCourant.adresseIp(requete));
        return ResponseEntity.ok(ApiResponse.ok("Dossier archive", archive));
    }
}
