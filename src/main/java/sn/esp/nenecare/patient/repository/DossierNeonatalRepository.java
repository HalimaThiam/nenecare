package sn.esp.nenecare.patient.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.esp.nenecare.patient.model.DossierNeonatal;

public interface DossierNeonatalRepository extends JpaRepository<DossierNeonatal, Long> {

    List<DossierNeonatal> findAllByOrderByDateNaissanceDesc();

    /** Tous les enfants d'une meme mere (liaison mere-enfant, OS-06). */
    List<DossierNeonatal> findByMereIdOrderByDateNaissanceDesc(Long mereId);

    boolean existsByNumeroDossier(String numeroDossier);

    long countByStatut(String statut);
}
