package sn.esp.nenecare.audit.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.audit.model.AuditLog;
import sn.esp.nenecare.audit.service.AuditService;
import sn.esp.nenecare.common.dto.ApiResponse;

/**
 * Consultation du journal d'audit (US-16, US-17).
 *
 * Proprietaire : Hadja (audit).
 *
 * Le journal expose qui a consulte quel dossier et depuis quelle adresse : il
 * est donc reserve a l'administrateur. La regle est posee a deux endroits
 * (SecurityConfig et @PreAuthorize) volontairement : si l'un des deux est
 * assoupli par erreur, l'autre tient encore.
 *
 * Le journal est en LECTURE SEULE : les entrees sont produites par les
 * operations metier (connexion, acces dossier...), jamais par cette API.
 */
@RestController
@RequestMapping("/api/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /** Journal complet, du plus recent au plus ancien. */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getTousLesLogs() {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getTousLesLogs()));
    }

    @GetMapping("/logs/utilisateur/{utilisateur}")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getLogsByUtilisateur(
            @PathVariable String utilisateur) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogsByUtilisateur(utilisateur)));
    }

    /** Tentatives echouees : connexions refusees, acces non autorises (US-17). */
    @GetMapping("/logs/echecs")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getEchecs() {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getEchecs()));
    }

    /** Filtrage par periode (US-17). Format attendu : 2026-07-28T00:00:00 */
    @GetMapping("/logs/periode")
    public ResponseEntity<ApiResponse<List<AuditLog>>> getLogsByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin) {
        return ResponseEntity.ok(ApiResponse.ok(auditService.getLogsByPeriode(debut, fin)));
    }

    /** Verifie la signature HMAC d'une entree precise (OS-08). */
    @GetMapping("/logs/{id}/integrite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifierIntegrite(
            @PathVariable Long id) {

        return auditService.getLog(id)
            .map(entree -> {
                boolean integre = auditService.verifierIntegrite(entree);
                return ResponseEntity.ok(ApiResponse.ok(
                    integre ? "Integrite verifiee - entree non alteree"
                            : "Integrite compromise - entree alteree !",
                    Map.<String, Object>of("id", id, "integre", integre)));
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Verifie l'ensemble du journal et liste les entrees suspectes. */
    @GetMapping("/integrite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifierToutLeJournal() {
        List<AuditLog> journal = auditService.getTousLesLogs();
        List<Long> alterees = journal.stream()
                .filter(entree -> !auditService.verifierIntegrite(entree))
                .map(AuditLog::getId)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
            alterees.isEmpty() ? "Journal integre" : "Alterations detectees !",
            Map.of("entrees", journal.size(),
                   "alterees", alterees.size(),
                   "idsAlterees", alterees)));
    }
}
