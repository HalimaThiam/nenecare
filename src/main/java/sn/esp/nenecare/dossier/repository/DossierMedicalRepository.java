package sn.esp.nenecare.dossier.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.esp.nenecare.dossier.entity.DossierMedical;

import java.util.List;
import java.util.UUID;

public interface DossierMedicalRepository extends JpaRepository<DossierMedical, UUID> {

    List<DossierMedical> findByPatientId(UUID patientId);

    boolean existsByPatientId(UUID patientId);
}
