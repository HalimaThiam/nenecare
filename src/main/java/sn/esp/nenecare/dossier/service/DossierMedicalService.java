package sn.esp.nenecare.dossier.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.esp.nenecare.dossier.dto.DossierMedicalRequest;
import sn.esp.nenecare.dossier.dto.DossierMedicalResponse;
import sn.esp.nenecare.dossier.entity.DossierMedical;
import sn.esp.nenecare.dossier.repository.DossierMedicalRepository;
import sn.esp.nenecare.exception.DossierNotFoundException;
import sn.esp.nenecare.patient.entity.Patient;
import sn.esp.nenecare.patient.repository.PatientRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DossierMedicalService {

    private final DossierMedicalRepository dossierRepository;
    private final PatientRepository patientRepository;

    public DossierMedicalResponse findById(UUID id) {
        DossierMedical dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new DossierNotFoundException(id));
        return toResponse(dossier);
    }

    public List<DossierMedicalResponse> findByPatient(UUID patientId) {
        return dossierRepository.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DossierMedicalResponse create(DossierMedicalRequest request, String username) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new EntityNotFoundException("Patient introuvable : " + request.patientId()));

        DossierMedical dossier = new DossierMedical();
        dossier.setPatient(patient);
        dossier.setCreePar(username);
        applyFields(dossier, request);

        return toResponse(dossierRepository.save(dossier));
    }

    @Transactional
    public DossierMedicalResponse update(UUID id, DossierMedicalRequest request, String username) {
        DossierMedical dossier = dossierRepository.findById(id)
                .orElseThrow(() -> new DossierNotFoundException(id));

        dossier.setModifiePar(username);
        applyFields(dossier, request);

        return toResponse(dossierRepository.save(dossier));
    }

    @Transactional
    public void delete(UUID id) {
        if (!dossierRepository.existsById(id)) {
            throw new DossierNotFoundException(id);
        }
        dossierRepository.deleteById(id);
    }

    // ── Helpers privés ────────────────────────────────────────────────────────

    private void applyFields(DossierMedical dossier, DossierMedicalRequest request) {
        dossier.setAntecedentsMedicaux(request.antecedentsMedicaux());
        dossier.setDiagnostics(request.diagnostics());
        dossier.setTraitements(request.traitements());
        dossier.setAllergies(request.allergies());
        dossier.setGroupeSanguin(request.groupeSanguin());

        // Notes confidentielles : seuls MEDECIN et ADMIN peuvent les écrire
        if (request.notesConfidentielles() != null && !canAccessConfidential()) {
            throw new AccessDeniedException("Seuls les médecins peuvent modifier les notes confidentielles");
        }
        dossier.setNotesConfidentielles(request.notesConfidentielles());
    }

    private DossierMedicalResponse toResponse(DossierMedical d) {
        String notesConfidentielles = canAccessConfidential() ? d.getNotesConfidentielles() : null;

        return new DossierMedicalResponse(
            d.getId(),
            d.getPatient().getId(),
            d.getPatient().getPrenom() + " " + d.getPatient().getNom(),
            d.getAntecedentsMedicaux(),
            d.getDiagnostics(),
            d.getTraitements(),
            d.getAllergies(),
            notesConfidentielles,
            d.getGroupeSanguin(),
            d.getDateCreation(),
            d.getDateMiseAJour(),
            d.getCreePar(),
            d.getModifiePar(),
            d.getVersion()
        );
    }

    private boolean canAccessConfidential() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_MEDECIN") || a.equals("ROLE_ADMIN"));
    }
}
