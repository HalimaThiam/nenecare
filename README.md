# NeneCare

Système sécurisé de gestion des dossiers médicaux d'une clinique mère-enfant
(Clinique Marose de Ouakam).

Projet DevSecOps – DIC2-SSI – ESP-UCAD 2025-2026
Responsable : Prof. Doudou FALL

## Équipe

| Membre | Rôle | Domaine technique |
|---|---|---|
| Halima THIAM (`HalimaThiam`) | Lead Sécurité & Architecte | `config/SecurityConfig`, RBAC/DAC, revue |
| Elimane KA (`thesombrecoder18`) | Dev Backend | `auth/`, `user/`, `common/` |
| Amadou DIAO (`diaopro95-glitch`) | Dev Backend | `patient/`, `crypto/` |
| Hadja DIALLO (`Hadja02`) | Dev Frontend & Audit/QA | `audit/`, `resources/static/` |

## Stack technique

- **Java 17 LTS** + **Spring Boot 3.5**
- Spring Security (RBAC + DAC), JWT (`jjwt`), bcrypt (coût 12)
- Bouncy Castle (AES-256-GCM, HMAC-SHA256, Kyber-768)
- PostgreSQL + Spring Data JPA
- Frontend : HTML/CSS/JS statique servi par Spring Boot (`resources/static`)

## Prérequis

- JDK 17 installé (`java -version` → 17.x)
- PostgreSQL en local, avec une base `nenecare`
- Git

```bash
# Créer la base (une seule fois)
createdb nenecare        # ou : psql -U postgres -c "CREATE DATABASE nenecare;"
```

## Configuration locale

Les secrets ne sont **pas** dans le dépôt. Définir avant le lancement (ou copier
`src/main/resources/application-local.properties.example`) :

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=votre_mot_de_passe
export JWT_SECRET=un-secret-de-32-caracteres-minimum
```

À défaut, des valeurs par défaut de développement sont utilisées (voir
`application.properties`).

## Lancer l'application

```bash
./mvnw spring-boot:run
```

- API : http://localhost:8081
- Page de connexion : http://localhost:8081/login.html

## Lancer les tests

```bash
./mvnw test        # utilise une base H2 en mémoire (pas besoin de PostgreSQL)
```

## Structure du code

```
src/main/java/sn/esp/nenecare/
├── config/     SecurityConfig (Halima), CryptoConfig (Amadou)
├── common/     DTO et gestion d'erreurs partagés (Elimane)
├── user/       Utilisateurs et rôles (Elimane + Halima)
├── auth/       Authentification, JWT (Elimane)
├── patient/    Dossiers médicaux et néonatals (Amadou)
├── crypto/     AES-GCM, HMAC, Kyber (Amadou)
└── audit/      Journal d'audit signé (Hadja)
src/main/resources/static/   Frontend HTML/CSS/JS (Hadja)
```

## Contribution

Voir [CONTRIBUTING.md](CONTRIBUTING.md) pour le workflow Git Flow et les conventions.
**Règle d'or : on ne pousse jamais directement sur `main` ni `develop`.**
