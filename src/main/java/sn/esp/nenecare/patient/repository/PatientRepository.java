package sn.esp.nenecare.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.esp.nenecare.patient.entity.Patient;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByNumeroDossier(String numeroDossier);

    // Note : la recherche par nom/prénom est impossible en SQL (champs chiffrés).
    // Pour une recherche nominative, implémenter côté service via chiffrement
    // déterministe ou index de recherche séparé (hors scope initial).
}
