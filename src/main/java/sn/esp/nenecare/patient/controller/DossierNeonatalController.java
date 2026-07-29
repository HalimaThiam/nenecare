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
import sn.esp.nenecare.patient.dto.DossierNeonatalRequest;
import sn.esp.nenecare.patient.dto.DossierNeonatalResponse;
import sn.esp.nenecare.patient.service.DossierNeonatalService;

/**
 * API des dossiers neonatals - US-10 a US-13.
 *
 * Proprietaire : Amadou (patient).
 *
 * RBAC applique ici (OS-05) :
 *  - REDIGER / MODIFIER : PEDIATRE, SAGE_FEMME.
 *  - CONSULTER : les redacteurs + INFIRMIER + ADMIN.
 *  - Le GYNECOLOGUE suit la mere, pas le nouveau-ne : il n'apparait pas ici.
 *    C'est le principe du moindre privilege applique aux specialites.
 */
@RestController
@RequestMapping("/api/neonatals")
@RequiredArgsConstructor
public class DossierNeonatalController {

    private static final String ROLES_LECTURE =
            "hasAnyRole('PEDIATRE','SAGE_FEMME','INFIRMIER','ADMIN')";
    private static final String ROLES_REDACTION =
            "hasAnyRole('PEDIATRE','SAGE_FEMME')";

    private final DossierNeonatalService dossierNeonatalService;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping
    @PreAuthorize(ROLES_LECTURE)
    public ResponseEntity<ApiResponse<List<DossierNeonatalResponse>>> lister(
            HttpServletRequest requete) {
        return ResponseEntity.ok(ApiResponse.ok(
                dossierNeonatalService.lister(utilisateurCourant.adresseIp(requete))));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ROLES_LECTURE)
    public ResponseEntity<ApiResponse<DossierNeonatalResponse>> consulter(
            @PathVariable Long id,
            @RequestParam(required = false) String motifUrgence,
            HttpServletRequest requete) {

        return ResponseEntity.ok(ApiResponse.ok(dossierNeonatalService.consulter(
                id, motifUrgence, utilisateurCourant.adresseIp(requete))));
    }

    /** Vue mere-enfant : tous les nouveau-nes rattaches a une patiente (OS-06). */
    @GetMapping("/mere/{mereId}")
    @PreAuthorize(ROLES_LECTURE)
    public ResponseEntity<ApiResponse<List<DossierNeonatalResponse>>> listerParMere(
            @PathVariable Long mereId,
            HttpServletRequest requete) {

        return ResponseEntity.ok(ApiResponse.ok(dossierNeonatalService.listerParMere(
                mereId, utilisateurCourant.adresseIp(requete))));
    }

    @PostMapping
    @PreAuthorize(ROLES_REDACTION)
    public ResponseEntity<ApiResponse<DossierNeonatalResponse>> creer(
            @Valid @RequestBody DossierNeonatalRequest corps,
            HttpServletRequest requete) {

        DossierNeonatalResponse cree = dossierNeonatalService.creer(
                corps, utilisateurCourant.adresseIp(requete));
        return ResponseEntity.ok(ApiResponse.ok("Dossier neonatal cree et chiffre", cree));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ROLES_REDACTION)
    public ResponseEntity<ApiResponse<DossierNeonatalResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody DossierNeonatalRequest corps,
            HttpServletRequest requete) {

        DossierNeonatalResponse modifie = dossierNeonatalService.modifier(
                id, corps, utilisateurCourant.adresseIp(requete));
        return ResponseEntity.ok(ApiResponse.ok("Dossier neonatal mis a jour", modifie));
    }
}
