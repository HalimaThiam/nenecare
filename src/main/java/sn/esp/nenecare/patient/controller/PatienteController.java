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
import sn.esp.nenecare.patient.dto.PatienteRequest;
import sn.esp.nenecare.patient.dto.PatienteResponse;
import sn.esp.nenecare.patient.service.PatienteService;

/**
 * API des patientes - US-05, US-06.
 *
 * Proprietaire : Amadou (patient).
 *
 * RBAC applique ici (OS-05) :
 *  - ENREGISTRER / MODIFIER une patiente : SECRETAIRE et ADMIN. C'est un acte
 *    d'accueil, pas un acte medical.
 *  - CONSULTER : tous les roles authentifies, mais le volet medical (groupe
 *    sanguin, antecedents) n'est renvoye qu'aux soignants - voir PatienteService.
 *
 * Le DAC vient en plus, dans le service : un gynecologue ne voit que ses
 * patientes referentes.
 */
@RestController
@RequestMapping("/api/patientes")
@RequiredArgsConstructor
public class PatienteController {

    private final PatienteService patienteService;
    private final UtilisateurCourant utilisateurCourant;

    @GetMapping
    @PreAuthorize("hasAnyRole('SECRETAIRE','GYNECOLOGUE','PEDIATRE','SAGE_FEMME','INFIRMIER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PatienteResponse>>> lister(
            HttpServletRequest requete) {
        return ResponseEntity.ok(ApiResponse.ok(
                patienteService.lister(utilisateurCourant.adresseIp(requete))));
    }

    /**
     * Consultation d'une fiche.
     *
     * @param motifUrgence renseigne uniquement pour un acces derogatoire a une
     *                     patiente non assignee (US-14). Toujours audite.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETAIRE','GYNECOLOGUE','PEDIATRE','SAGE_FEMME','INFIRMIER','ADMIN')")
    public ResponseEntity<ApiResponse<PatienteResponse>> consulter(
            @PathVariable Long id,
            @RequestParam(required = false) String motifUrgence,
            HttpServletRequest requete) {

        return ResponseEntity.ok(ApiResponse.ok(patienteService.consulter(
                id, motifUrgence, utilisateurCourant.adresseIp(requete))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN')")
    public ResponseEntity<ApiResponse<PatienteResponse>> creer(
            @Valid @RequestBody PatienteRequest corps,
            HttpServletRequest requete) {

        PatienteResponse creee = patienteService.creer(
                corps, utilisateurCourant.adresseIp(requete));
        return ResponseEntity.ok(ApiResponse.ok("Patiente enregistree", creee));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SECRETAIRE','ADMIN')")
    public ResponseEntity<ApiResponse<PatienteResponse>> modifier(
            @PathVariable Long id,
            @Valid @RequestBody PatienteRequest corps,
            HttpServletRequest requete) {

        PatienteResponse modifiee = patienteService.modifier(
                id, corps, utilisateurCourant.adresseIp(requete));
        return ResponseEntity.ok(ApiResponse.ok("Fiche mise a jour", modifiee));
    }
}
