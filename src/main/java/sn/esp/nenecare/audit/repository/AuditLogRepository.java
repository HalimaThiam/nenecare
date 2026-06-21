package sn.esp.nenecare.audit.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.esp.nenecare.audit.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUtilisateur(String utilisateur);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTimestampBetween(LocalDateTime debut, LocalDateTime fin);

    List<AuditLog> findByUtilisateurAndTimestampBetween(
        String utilisateur, LocalDateTime debut, LocalDateTime fin);

    List<AuditLog> findBySucces(Boolean succes);

    List<AuditLog> findByRole(String role);
}
