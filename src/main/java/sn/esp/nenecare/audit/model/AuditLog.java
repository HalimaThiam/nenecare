package sn.esp.nenecare.audit.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entree du journal d'audit, horodatee et signee HMAC (OS-08).
 * Stockee dans une table isolee, en lecture seule pour les utilisateurs ordinaires.
 *
 * Proprietaire : Hadja (audit).
 */
@Entity
@Table(name = "audit_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false, length = 100)
    private String utilisateur;

    @Column(nullable = false, length = 50)
    private String role;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 200)
    private String ressource;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Column(nullable = false)
    private Boolean succes;

    @Column(name = "signature_hmac", nullable = false, length = 500)
    private String signatureHmac;
}
