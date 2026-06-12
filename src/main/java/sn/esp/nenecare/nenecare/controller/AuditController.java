package sn.esp.nenecare.nenecare.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.nenecare.model.AuditLog;
import sn.esp.nenecare.nenecare.service.AuditService;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    // Récupère tous les logs (administrateur uniquement)
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getTousLesLogs() {
        return ResponseEntity.ok(auditService.getTousLesLogs());
    }

    // Récupère les logs d'un utilisateur spécifique
    @GetMapping("/logs/utilisateur/{utilisateur}")
    public ResponseEntity<List<AuditLog>> getLogsByUtilisateur(
            @PathVariable String utilisateur) {
        return ResponseEntity.ok(
            auditService.getLogsByUtilisateur(utilisateur)
        );
    }

    // Récupère les tentatives échouées
    @GetMapping("/logs/echecs")
    public ResponseEntity<List<AuditLog>> getEchecs() {
        return ResponseEntity.ok(auditService.getEchecs());
    }

    // Teste l'enregistrement d'un log (pour démonstration)
    @PostMapping("/test")
    public ResponseEntity<AuditLog> testerAudit(
            @RequestParam String utilisateur,
            @RequestParam String role,
            @RequestParam String action,
            @RequestParam(required = false) String ressource,
            @RequestParam(required = false) String details) {

        AuditLog log = auditService.logAction(
            utilisateur, role, action,
            ressource, details, "127.0.0.1", true
        );
        return ResponseEntity.ok(log);
    }

    // Vérifie l'intégrité d'un log par son ID
    @GetMapping("/logs/{id}/integrite")
    public ResponseEntity<String> verifierIntegrite(@PathVariable Long id) {
        List<AuditLog> logs = auditService.getTousLesLogs();
        AuditLog log = logs.stream()
            .filter(l -> l.getId().equals(id))
            .findFirst()
            .orElse(null);

        if (log == null) {
            return ResponseEntity.notFound().build();
        }

        boolean integre = auditService.verifierIntegrite(log);
        return ResponseEntity.ok(
            integre ? "✅ Intégrité vérifiée — log non altéré"
                    : "❌ Intégrité compromise — log altéré !"
        );
    }
}