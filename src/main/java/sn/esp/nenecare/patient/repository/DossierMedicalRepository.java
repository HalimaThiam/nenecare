package sn.esp.nenecare.patient.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.esp.nenecare.patient.model.DossierMedical;

public interface DossierMedicalRepository extends JpaRepository<DossierMedical, Long> {

    /** DAC : un gynecologue ne recupere que ses patientes assignees (US-07). */
    List<DossierMedical> findByGynecologueAssigne(String gynecologueAssigne);
}
