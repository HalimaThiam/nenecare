# NeneCare

Système sécurisé de gestion des dossiers médicaux d'une clinique mère-enfant
(Clinique Marose de Ouakam).

Projet DevSecOps – DIC2-SSI – ESP-UCAD 2025-2026
Responsable : Prof. Doudou FALL

---

## 1. Équipe & répartition

| Membre | Rôle | Branche | Domaine technique |
|---|---|---|---|
| Halima THIAM (`HalimaThiam`) | Lead Sécurité & Architecte | `feature/security` | `config/`, `user/`, filtre JWT, RBAC, **coordination des merges** |
| Elimane KA (`thesombrecoder18`) | Dev Backend – Auth | `feature/auth` | `auth/`, `common/` |
| Amadou DIAO (`diaopro95-glitch`) | Dev Backend – Dossiers | `feature/dossiers` | `patient/`, `crypto/` |
| Hadja DIALLO (`Hadja02`) | Dev Frontend & Audit/QA | `feature/audit` | `audit/`, `resources/static/` |

## 2. Stack technique

- **Java 17 LTS** + **Spring Boot 3.5**
- Spring Security (RBAC + DAC), JWT (`jjwt`), bcrypt (coût 12)
- Bouncy Castle (AES-256-GCM, HMAC-SHA256, Kyber-768)
- PostgreSQL + Spring Data JPA
- Frontend : HTML/CSS/JS statique servi par Spring Boot (`resources/static`)

---

## 3. Démarrage

### 3.1 Démarrage rapide (aucune installation de base de données)

Pour tester l'application, faire une démo ou développer le frontend, le profil `dev`
utilise une base **H2 en mémoire** créée au lancement :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Application : <http://localhost:8081> (redirige vers la connexion)
- Console base : <http://localhost:8081/h2-console> (JDBC `jdbc:h2:mem:nenecare`, user `sa`)

Six **comptes de démonstration** sont créés au démarrage, un par rôle
(mot de passe commun : `NeneCare2026!`) :

| Identifiant | Rôle | Voit le journal d'audit |
|---|---|---|
| `admin` | ADMIN | ✅ |
| `gyneco` | GYNECOLOGUE | ❌ (403) |
| `pediatre` | PEDIATRE | ❌ |
| `sagefemme` | SAGE_FEMME | ❌ |
| `infirmier` | INFIRMIER | ❌ |
| `secretaire` | SECRETAIRE | ❌ |

> ⚠️ Ces comptes n'existent **que** si `nenecare.demo.enabled=true` — donc uniquement
> dans le profil `dev`. La base est vidée à chaque arrêt de l'application.

### 3.2 Démarrage sur PostgreSQL (configuration par défaut)

> **Prérequis : JDK 17** (`java -version` → 17.x), **PostgreSQL**, **Git**.
> ⚠️ Si ta machine a une autre version de Java (ex. 21/26), installe le JDK 17 pour rester
> conforme à la charte et éviter des écarts de comportement.

```bash
# 1. Récupérer le projet et la base d'intégration
git clone https://github.com/HalimaThiam/nenecare.git
cd nenecare
git checkout develop
git pull origin develop

# 2. Créer SA branche de travail (depuis develop)
git checkout -b feature/<ton-domaine>     # ex: feature/auth

# 3. Configurer les secrets (au choix : variables d'env OU fichier local)
export DB_USERNAME=postgres
export DB_PASSWORD=ton_mot_de_passe
export JWT_SECRET=un-secret-de-32-caracteres-minimum        # >= 32 caractères, sinon l'app refuse de démarrer
export AUDIT_HMAC_KEY=$(openssl rand -base64 32)            # clé de signature du journal d'audit
#   (alternative : copier src/main/resources/application-local.properties.example
#    en application-local.properties — ce fichier est ignoré par git)

# 4. Créer la base PostgreSQL (une seule fois)
createdb nenecare        # ou : psql -U postgres -c "CREATE DATABASE nenecare;"

# 5. Lancer l'application
./mvnw spring-boot:run
#   API            : http://localhost:8081
#   Page de login  : http://localhost:8081/login.html

# 6. Avant CHAQUE Pull Request : vérifier que le build est vert
./mvnw test              # tourne sur une base H2 en mémoire (pas besoin de PostgreSQL)
```

---

## 4. Structure du code

```
src/main/java/sn/esp/nenecare/
├── config/     SecurityConfig (Halima), CryptoConfig (Amadou)
├── common/     ApiResponse + gestion d'erreurs partagées (Elimane)
├── user/       User + Role (Elimane + Halima)
├── auth/       Authentification, JWT (Elimane)
├── patient/    Dossiers médicaux et néonatals (Amadou)
├── crypto/     AES-GCM, HMAC, Kyber (Amadou)
└── audit/      Journal d'audit signé HMAC (Hadja)
src/main/resources/static/   Frontend HTML/CSS/JS (Hadja)
```

---

## 5. Ta zone de travail (par membre)

### 🔐 Halima — Lead Sécurité
- **Dossiers** : `config/`, `user/`, `auth/jwt/`
- **À faire** :
  - Créer `auth/jwt/JwtAuthFilter.java` (lit `Authorization: Bearer ...`, valide via
    `JwtService`, place l'utilisateur dans le `SecurityContext`) et **le brancher** dans
    `config/SecurityConfig.java` (`http.addFilterBefore(...)`).
  - Compléter l'entité `user/model/User.java` (rôle, mot de passe bcrypt, `actif`) — déjà amorcée.
  - Poser le RBAC : annoter les contrôleurs de chacun avec `@PreAuthorize` (voir §6).
  - **Coordonner les merges** : relire et valider les PR vers `develop`.

### 🪪 Elimane — Auth & Autorisation
- **Dossiers** : `auth/`, `common/`
- **À faire** (fichiers déjà amorcés) :
  - `auth/controller/AuthController.java` + `auth/service/AuthService.java` :
    `POST /api/auth/login` (vérif bcrypt → JWT) et `POST /api/auth/logout` (révocation).
  - `auth/service/LoginAttemptService.java` : blocage après 5 échecs (US-04) + entrée d'audit.
  - `auth/jwt/JwtService.java` : expiration 30 min (US-02) — déjà câblé, à finaliser.

### 🗂️ Amadou — Dossiers & Chiffrement
- **Dossiers** : `patient/`, `crypto/`
- **À faire** :
  - Entités JPA : compléter `patient/model/DossierMedical.java`, créer `Patiente.java` et
    `DossierNeonatal.java`, avec la **liaison mère-enfant** (`@ManyToOne` / `@OneToOne`).
  - CRUD dans `patient/service/` + endpoints REST dans `patient/controller/`.
  - **Chiffrer** les champs médicaux sensibles avec `crypto/AesGcmService` AVANT `save`,
    déchiffrer après lecture ; signer/vérifier l'intégrité avec `crypto/HmacService` (voir §6).

### 🖥️ Hadja — Frontend & QA
- **Dossiers** : `src/main/resources/static/`, `audit/`
- **À faire** :
  - `static/login.html` (déjà fonctionnel) à finaliser ; page d'accueil `index.html` **adaptée
    au rôle** connecté (lire le rôle depuis la réponse de login).
  - Utiliser le helper `static/js/api.js` (`NeneCareApi`) pour tous les appels.
  - Tester les endpoints (curl / Postman) et **mettre à jour le document d'assurance**.
  - Le module `audit/` est déjà fonctionnel (consultation des journaux).

---

## 6. Règles de cohérence (à respecter par tous)

Pour que les 4 contributions s'assemblent sans friction, **réutilise les briques déjà écrites** :

| Sujet | Règle | Où |
|---|---|---|
| **Réponses REST** | Toujours envelopper dans `ApiResponse.ok(...)` / `ApiResponse.erreur(...)` | `common/dto/ApiResponse.java` |
| **Erreurs** | Ne jamais renvoyer de stacktrace ; lever une exception, le handler renvoie un message clair non technique (US-22) | `common/exception/GlobalExceptionHandler.java` |
| **RBAC** | Annoter les endpoints : `@PreAuthorize("hasRole('GYNECOLOGUE')")` | activé par `@EnableMethodSecurity` dans `config/SecurityConfig.java` |
| **DAC** | Filtrer par propriétaire : un médecin ne voit que ses patientes assignées | ex. `DossierMedicalRepository.findByGynecologueAssigne` |
| **Chiffrement** | Données médicales chiffrées `AesGcmService.encrypt(...)` **avant** `save`, `decrypt(...)` après lecture — jamais de clair en base (OS-03) | `crypto/AesGcmService.java` |
| **Intégrité** | `HmacService.sign(...)` à la création, `verify(...)` à la lecture (OS-07) | `crypto/HmacService.java` |
| **Audit** | Appeler `AuditService.logAction(...)` sur toute action sensible (connexion, accès/modif/suppression dossier, accès d'urgence) | `audit/service/AuditService.java` |
| **JWT** | `JwtService` génère/valide ; `JwtAuthFilter` (Halima) authentifie chaque requête | `auth/jwt/` |
| **Frontend** | Passer par `NeneCareApi` (jeton en `sessionStorage`, réponses au format `ApiResponse`) | `static/js/api.js` |

> ⚠️ **Piège RBAC Spring** : `hasRole('ADMIN')` attend l'authority `ROLE_ADMIN`. Quand on
> construit le `UserDetails` / les authorities depuis l'enum `Role`, préfixer par `ROLE_`
> (ex. `new SimpleGrantedAuthority("ROLE_" + user.getRole().name())`).

**Fichiers partagés — ne pas modifier seul, prévenir sur WhatsApp :**
`config/SecurityConfig.java`, `user/model/User.java`, `user/model/Role.java`,
`common/dto/ApiResponse.java`.

---

## 7. Flux Git de l'équipe

Détails complets dans **[CONTRIBUTING.md](CONTRIBUTING.md)**. L'essentiel :

- **Jamais de push direct sur `main` ni `develop`.**
- Travailler sur sa branche `feature/*`, puis ouvrir une **Pull Request vers `develop`**.
- **Halima relit et valide** chaque merge.
- Se resynchroniser souvent : `git checkout develop && git pull`, puis `git merge develop`
  dans sa branche.
- Messages de commit préfixés par domaine : `auth:`, `crypto:`, `patient:`, `audit:`, `front:`,
  `config:`, `docs:`.

---

## 8. État d'avancement

| Fonctionnalité | US / OS | Statut |
|---|---|---|
| Socle Spring Boot + structure packages | — | ✅ fait |
| Sécurité de base (bcrypt coût 12, `@EnableMethodSecurity`) | OS-12 | ✅ fait |
| Crypto AES-256-GCM + HMAC | OS-03 / OS-07 | ✅ services prêts + testés |
| Journal d'audit signé (écriture, consultation, vérification) | US-16/17, OS-08 | ✅ fait |
| `POST /api/auth/login` (JWT + audit) | US-01 | ✅ fait |
| `POST /api/auth/logout` (révocation par `jti`) | US-03 | ✅ fait |
| Expiration du jeton à 30 min | US-02 | ✅ fait |
| Blocage après 5 échecs, fenêtre de 15 min | US-04 | ✅ fait |
| Filtre JWT branché dans Security | — | ✅ fait |
| RBAC sur le journal d'audit (`ADMIN` seul) | OS-05 | ✅ fait |
| Frontend login + accueil branchés sur l'API | — | ✅ fait |
| Suite de tests (39 tests, `./mvnw test`) | — | ✅ fait |
| Entités Patiente / DossierMedical / DossierNeonatal + liaison | US-05→13, OS-06 | 🔲 à faire (Amadou) |
| CRUD dossiers chiffrés + endpoints REST | US-07/08 | 🔲 à faire (Amadou) |
| RBAC `@PreAuthorize` sur les endpoints dossiers | OS-05 | 🔲 à faire (avec les dossiers) |
| Écrans dossiers / patientes | — | 🔲 à faire (Hadja) |
| Gestion des comptes (création, révocation) | US-18/19 | 🔲 à faire (Halima) |

Légende : ✅ fait · 🟡 amorcé (stub) · 🔲 à faire.

### Reste à durcir (phase sécurité)

Ces points sont identifiés et **volontairement** repoussés à la passe suivante :

| Sujet | À faire |
|---|---|
| Migrations | Ajouter Flyway et passer `ddl-auto` à `validate` (aujourd'hui `update`) |
| Immuabilité du journal | Appliquer les `GRANT`/`REVOKE` de `V1__create_audit_log.sql` sur PostgreSQL |
| Révocation des jetons | Liste en mémoire → Redis ou table (perdue au redémarrage, non partagée) |
| Blocage des comptes | Compteurs en mémoire → persistance |
| Frontend | Auto-héberger les polices Google, ajouter une CSP |
| Transport | Forcer HTTPS + HSTS |
| Kyber | `BouncyCastlePQCProvider` déprécié → ML-KEM du provider principal |
| CI | Pipeline GitHub Actions (`mvnw test` + scan de dépendances) |
