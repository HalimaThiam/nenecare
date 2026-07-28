package sn.esp.nenecare.audit.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.audit.model.AuditLog;
import sn.esp.nenecare.audit.service.AuditService;

/**
 * Consultation du journal d'audit (US-16, US-17).
 * A terme : reserve a l'administrateur via @PreAuthorize("hasRole('ADMIN')").
 *
 * Proprietaire : Hadja (audit).
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getTousLesLogs() {
        return ResponseEntity.ok(auditService.getTousLesLogs());
    }

    @GetMapping("/logs/utilisateur/{utilisateur}")
    public ResponseEntity<List<AuditLog>> getLogsByUtilisateur(@PathVariable String utilisateur) {
        return ResponseEntity.ok(auditService.getLogsByUtilisateur(utilisateur));
    }

    @GetMapping("/logs/echecs")
    public ResponseEntity<List<AuditLog>> getEchecs() {
        return ResponseEntity.ok(auditService.getEchecs());
    }

    /** Test d'enregistrement d'un log (demo Sprint Alpha). */
    @PostMapping("/test")
    public ResponseEntity<AuditLog> testerAudit(
            @RequestParam String utilisateur,
            @RequestParam String role,
            @RequestParam String action,
            @RequestParam(required = false) String ressource,
            @RequestParam(required = false) String details) {
        AuditLog log = auditService.logAction(
            utilisateur, role, action, ressource, details, "127.0.0.1", true);
        return ResponseEntity.ok(log);
    }

    @GetMapping("/logs/{id}/integrite")
    public ResponseEntity<String> verifierIntegrite(@PathVariable Long id) {
        AuditLog log = auditService.getTousLesLogs().stream()
            .filter(l -> l.getId().equals(id))
            .findFirst()
            .orElse(null);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }
        boolean integre = auditService.verifierIntegrite(log);
        return ResponseEntity.ok(integre
            ? "Integrite verifiee - log non altere"
            : "Integrite compromise - log altere !");
    }
}
