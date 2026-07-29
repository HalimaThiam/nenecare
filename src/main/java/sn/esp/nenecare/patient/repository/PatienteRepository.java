package sn.esp.nenecare.patient.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.esp.nenecare.patient.model.Patiente;

public interface PatienteRepository extends JpaRepository<Patiente, Long> {

    Optional<Patiente> findByNumeroPatiente(String numeroPatiente);

    boolean existsByNumeroPatiente(String numeroPatiente);

    List<Patiente> findAllByOrderByNomAscPrenomAsc();

    /** DAC : les patientes referencees par un gynecologue precis (US-07). */
    List<Patiente> findByGynecologueAssigneOrderByNomAsc(String gynecologueAssigne);

    long countByGynecologueAssigne(String gynecologueAssigne);
}
