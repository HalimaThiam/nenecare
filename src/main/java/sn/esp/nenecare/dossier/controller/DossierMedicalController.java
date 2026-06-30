package sn.esp.nenecare.dossier.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.esp.nenecare.dossier.dto.DossierMedicalRequest;
import sn.esp.nenecare.dossier.dto.DossierMedicalResponse;
import sn.esp.nenecare.dossier.service.DossierMedicalService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dossiers")
@RequiredArgsConstructor
public class DossierMedicalController {

    private final DossierMedicalService service;

    // ── Lecture ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN')")
    public ResponseEntity<DossierMedicalResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'INFIRMIER', 'ADMIN')")
    public ResponseEntity<List<DossierMedicalResponse>> getByPatient(@PathVariable UUID patientId) {
        return ResponseEntity.ok(service.findByPatient(patientId));
    }

    // ── Écriture ──────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN')")
    public ResponseEntity<DossierMedicalResponse> create(
            @Valid @RequestBody DossierMedicalRequest request,
            Authentication authentication) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.create(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN', 'ADMIN')")
    public ResponseEntity<DossierMedicalResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody DossierMedicalRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(service.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
