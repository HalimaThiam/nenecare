package sn.esp.nenecare.audit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.esp.nenecare.audit.model.AuditLog;

/**
 * Acces au journal d'audit.
 *
 * Uniquement des lectures et des insertions : aucune methode de modification
 * ni de suppression n'est exposee, pour preserver la valeur probante du
 * journal (OS-08).
 *
 * Proprietaire : Hadja (audit).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** Journal complet, evenement le plus recent en premier. */
    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByUtilisateur(String utilisateur);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTimestampBetween(LocalDateTime debut, LocalDateTime fin);

    List<AuditLog> findByUtilisateurAndTimestampBetween(
        String utilisateur, LocalDateTime debut, LocalDateTime fin);

    List<AuditLog> findBySucces(Boolean succes);

    List<AuditLog> findByRole(String role);

    /** Echecs recents d'un identifiant : detection de force brute (US-04). */
    List<AuditLog> findByUtilisateurAndActionAndTimestampAfter(
        String utilisateur, String action, LocalDateTime depuis);
}
