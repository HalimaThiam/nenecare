# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**NeneCare** is a secure medical records management system for a mother-and-child clinic, built as a DevSecOps academic project (M1-SSI, ESP-UCAD 2025-2026) under Prof. Doudou FALL.

Security is a first-class concern throughout: all design and implementation decisions should reflect the sensitivity of medical data (PHI/PII), regulatory constraints, and the DevSecOps mandate of the course.

## Team Roles

| Handle | Responsibility |
|---|---|
| HalimaThiam | Security Lead & Architect |
| thesombrecoder18 | Backend – Auth & Authorization |
| diaopro95-glitch | Backend – Medical Records & Encryption |
| Hadja02 | Frontend & Audit / QA |

## Stack

- **Java 17** / **Spring Boot 3.2.5**
- **PostgreSQL** (production), H2 (tests)
- **Spring Security 6** — stateless JWT (filtre JWT à intégrer par thesombrecoder18)
- **Spring Data JPA** / Hibernate 6
- **Lombok**, Bean Validation

## Build, Test, et Run

```bash
# Compiler
mvn clean package -DskipTests

# Lancer (nécessite NENECARE_ENCRYPTION_KEY et DB_PASSWORD dans l'env)
mvn spring-boot:run

# Tests
mvn test

# Test unitaire unique
mvn test -Dtest=NomDeLaClasse#nomDuTest
```

Copier `.env.example` en `.env` et renseigner les variables avant de lancer.
Générer la clé AES-256 : `openssl rand -base64 32`

## Architecture

### Structure des packages

```
sn.esp.nenecare
├── config/          — SecurityConfig, ApplicationContextProvider
├── encryption/      — AesGcmEncryptor, EncryptedStringConverter
├── patient/         — entité Patient + PatientRepository
├── dossier/         — entité DossierMedical, DTO, service, controller, repository
├── audit/           — DossierAuditListener (JPA entity listener)
└── exception/       — GlobalExceptionHandler, DossierNotFoundException
```

### Chiffrement applicatif (AES-256-GCM)

`AesGcmEncryptor` chiffre/déchiffre les champs sensibles. Format stocké en base :
`Base64( IV[12 octets] || ChiffréGCM[N octets] || Tag[16 octets] )`

Un IV aléatoire est généré **à chaque chiffrement** via `SecureRandom` — critique en GCM pour éviter la réutilisation IV+clé.

`EncryptedStringConverter` est un `AttributeConverter<String,String>` JPA appliqué avec `@Convert(converter = EncryptedStringConverter.class)` sur les champs d'entité. Hibernate l'instancie hors contexte Spring ; il accède au bean `AesGcmEncryptor` via `ApplicationContextProvider` (holder statique initialisé au démarrage, avant JPA).

**Champs chiffrés dans `DossierMedical`** : `antecedentsMedicaux`, `diagnostics`, `traitements`, `allergies`, `notesConfidentielles`.
**Non chiffré** : `groupeSanguin` (lisible en urgence), dates, IDs, statuts.

**Champs chiffrés dans `Patient`** : `nom`, `prenom`, `telephone`, `adresse`.
**Non chiffré** : `numeroDossier` (recherche/listes), `dateNaissance`, `sexe`.

> Conséquence : **aucune requête SQL LIKE sur les champs chiffrés**. La recherche par nom de patient doit être implémentée côté applicatif ou via un index de recherche séparé.

### Sécurité & autorisation

Contrôle d'accès en deux couches :
1. **`@PreAuthorize`** sur les endpoints — rôles : `MEDECIN`, `INFIRMIER`, `ADMIN`
2. **Service** — `notesConfidentielles` masquées pour `INFIRMIER` (lecture) et rejetées en écriture

Le filtre JWT est un placeholder dans `SecurityConfig` — à compléter par thesombrecoder18.

### Audit

`DossierAuditListener` (JPA `@EntityListeners`) logue CREATE/UPDATE/DELETE avec l'utilisateur courant (`SecurityContextHolder`). Phase 2 (Hadja02) : persister en table `audit_logs`.

### Verrou optimiste

Le champ `@Version Long version` sur `DossierMedical` prévient les écrasements concurrents de dossiers médicaux.
