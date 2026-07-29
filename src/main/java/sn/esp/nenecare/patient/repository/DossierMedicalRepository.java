package sn.esp.nenecare.patient.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.esp.nenecare.patient.model.DossierMedical;

public interface DossierMedicalRepository extends JpaRepository<DossierMedical, Long> {

    List<DossierMedical> findAllByOrderByDateConsultationDesc();

    /**
     * DAC : un gynecologue ne recupere que les dossiers des patientes qui lui
     * sont assignees (US-07). Le filtre est applique par la BASE, pas apres
     * coup en Java : les dossiers des autres patientes ne remontent jamais
     * jusqu'a la couche applicative.
     */
    List<DossierMedical> findByPatienteGynecologueAssigneOrderByDateConsultationDesc(
            String gynecologueAssigne);

    List<DossierMedical> findByPatienteIdOrderByDateConsultationDesc(Long patienteId);

    boolean existsByNumeroDossier(String numeroDossier);

    long countByStatut(String statut);
}
