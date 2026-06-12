package sn.esp.nenecare.nenecare.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.esp.nenecare.nenecare.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Recherche par utilisateur
    List<AuditLog> findByUtilisateur(String utilisateur);

    // Recherche par action
    List<AuditLog> findByAction(String action);

    // Recherche par période
    List<AuditLog> findByTimestampBetween(
        LocalDateTime debut, LocalDateTime fin
    );

    // Recherche par utilisateur et période
    List<AuditLog> findByUtilisateurAndTimestampBetween(
        String utilisateur, LocalDateTime debut, LocalDateTime fin
    );

    // Recherche des échecs uniquement
    List<AuditLog> findBySucces(Boolean succes);

    // Recherche par rôle
    List<AuditLog> findByRole(String role);
}