package com.nenecare.audit.repository;

import com.nenecare.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository du journal d'audit NeneCare.
 *
 * Toutes les méthodes sont en lecture seule (pas de delete/update exposé)
 * pour préserver l'intégrité forensique du journal.
 *
 * Sprint Alpha – feature/audit
 * Responsable : Hadja Mariama DIALLO
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // -------------------------------------------------------------------------
    // Requêtes par acteur
    // -------------------------------------------------------------------------

    /** Toutes les actions d'un utilisateur, les plus récentes en premier. */
    Page<AuditLog> findByActorIdOrderByTimestampDesc(String actorId, Pageable pageable);

    /** Actions d'un utilisateur filtrées par type d'action. */
    List<AuditLog> findByActorIdAndAction(String actorId, String action);

    // -------------------------------------------------------------------------
    // Requêtes par ressource
    // -------------------------------------------------------------------------

    /**
     * Historique complet d'une ressource donnée.
     * Ex : tous les accès au DossierMedical #42.
     */
    List<AuditLog> findByResourceTypeAndResourceIdOrderByTimestampAsc(
            String resourceType, String resourceId);

    // -------------------------------------------------------------------------
    // Requêtes par action
    // -------------------------------------------------------------------------

    /** Toutes les entrées pour un code d'action donné, paginées. */
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);

    // -------------------------------------------------------------------------
    // Requêtes temporelles
    // -------------------------------------------------------------------------

    /** Entrées dans une plage de temps. Utile pour les rapports périodiques. */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :from AND :to ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(@Param("from") Instant from,
                                           @Param("to")   Instant to);

    /**
     * Entrées récentes d'un acteur sur une ressource, dans une fenêtre de temps.
     * Utilisé pour détecter les accès répétés suspects.
     */
    @Query("""
            SELECT a FROM AuditLog a
            WHERE a.actorId      = :actorId
              AND a.resourceType = :resourceType
              AND a.timestamp   >= :since
            ORDER BY a.timestamp DESC
            """)
    List<AuditLog> findRecentAccessByActor(@Param("actorId")      String  actorId,
                                            @Param("resourceType") String  resourceType,
                                            @Param("since")        Instant since);

    // -------------------------------------------------------------------------
    // Requêtes de sécurité
    // -------------------------------------------------------------------------

    /** Nombre d'échecs de connexion pour un acteur depuis une date donnée. */
    @Query("""
            SELECT COUNT(a) FROM AuditLog a
            WHERE a.actorId   = :actorId
              AND a.action    = 'LOGIN_FAILURE'
              AND a.timestamp >= :since
            """)
    long countLoginFailuresSince(@Param("actorId") String  actorId,
                                  @Param("since")   Instant since);

    /** Toutes les tentatives de connexion échouées, les plus récentes d'abord. */
    Page<AuditLog> findByActionOrderByTimestampDesc(
            @SuppressWarnings("unused") String action, // "LOGIN_FAILURE"
            Pageable pageable,
            @SuppressWarnings("unused") String unused  // surcharge JPA non utilisée
    );
}
