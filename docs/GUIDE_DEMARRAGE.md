# Guide de démarrage NeneCare (par rôle)

Ce guide explique, étape par étape, comment chaque membre installe la base de données,
lance le projet, et où se trouve son travail. À lire avant de commencer.

---

## 0. Pré-requis (tout le monde)

- **JDK 17** installé → `java -version` doit afficher `17.x`
- **PostgreSQL** installé et démarré *(inutile si tu utilises le raccourci ci-dessous)*
- **Git**

---

## 0 bis. Le raccourci : lancer sans installer PostgreSQL

Si tu veux juste **voir tourner l'application** (démo, test d'un endpoint, travail sur
le frontend), le profil `dev` crée une base H2 en mémoire toute seule :

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Puis ouvre <http://localhost:8081> et connecte-toi avec un des comptes créés
automatiquement (mot de passe commun `NeneCare2026!`) :

`admin` · `gyneco` · `gyneco2` · `pediatre` · `sagefemme` · `infirmier` · `secretaire`

(`gyneco` et `gyneco2` existent tous les deux pour pouvoir démontrer le DAC :
chacun ne voit que ses propres patientes, voir section 3 ci-dessous.)

Seul `admin` a accès au journal d'audit — les autres reçoivent un 403, c'est voulu.

> La base disparaît à l'arrêt de l'application. Pour conserver tes données entre deux
> lancements, passe à PostgreSQL (section 1).

### Vérifier que tout marche, en ligne de commande

```bash
curl -s -X POST http://localhost:8081/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","motDePasse":"NeneCare2026!"}'
```

La réponse contient un `token` : réutilise-le en en-tête `Authorization: Bearer <token>`
pour appeler les endpoints protégés, par exemple `GET /api/audit/logs`.

---

## 1. Setup de la base de données (identique pour tous)

Chaque membre exécute ces commandes **une seule fois** sur sa machine.

```bash
# 1. Vérifier que PostgreSQL répond (mot de passe de l'utilisateur postgres demandé)
psql -U postgres -h localhost -c "SELECT version();"

# 2. Créer la base de données du projet
createdb -U postgres -h localhost nenecare
#   (alternative si createdb indisponible :)
#   psql -U postgres -h localhost -c "CREATE DATABASE nenecare;"

# 3. Vérifier qu'elle existe
psql -U postgres -h localhost -lqt | grep nenecare
```

> Pas besoin de créer les tables à la main : Spring/Hibernate les génère automatiquement
> au premier lancement (`spring.jpa.hibernate.ddl-auto=update`).

### Configuration des secrets (.env)

```bash
# Copier le modèle, puis adapter les valeurs
cp .env.example .env
```

Éditer `.env` :
- `DB_USERNAME` → généralement `postgres`
- `DB_PASSWORD` → **ton** mot de passe PostgreSQL local
- `JWT_SECRET` → laisser celui fourni, ou en générer un : `openssl rand -base64 48`

Le fichier `.env` est **ignoré par git** : tes secrets restent sur ta machine.

---

## 2. Lancer le projet (identique pour tous)

```bash
# Récupérer la dernière version d'intégration
git checkout develop && git pull origin develop

# Charger les variables du .env puis démarrer
set -a && source .env && set +a && ./mvnw spring-boot:run
```

- **Application** : http://localhost:8081
- **Page de connexion (ce qui s'affiche par défaut)** : http://localhost:8081/login.html

Pour vérifier que le build est sain (sans PostgreSQL, sur base H2) :
```bash
./mvnw test
```

---

## 3. Guide par rôle

### 🖥️ Hadja — Dev Frontend & QA

**Ce qui s'affiche par défaut**
Quand tu ouvres http://localhost:8081/login.html, tu vois la **page de connexion**
(logo NeneCare, champs identifiant + mot de passe, bouton « Se connecter »). Après une
connexion réussie, l'utilisateur est redirigé vers la page d'accueil `index.html`.

**Où sont les fichiers**
```
src/main/resources/static/
├── login.html         → page de connexion
├── accueil.html       → tableau de bord (KPIs + audit récent, adapté au rôle)
├── dossiers.html      → patientes / dossiers médicaux / dossiers néonatals (US-05→14)
├── audit.html         → journal d'audit complet (ADMIN uniquement)
├── css/app.css        → style partagé par accueil/dossiers/audit
└── js/
    ├── api.js         → helper d'appel à l'API (objet NeneCareApi : get/post/put/login/logout)
    └── shell.js        → barre latérale + navigation adaptée au rôle (NeneCareShell)
```

**Comment travailler**
- Tu modifies uniquement le dossier `static/` → aucun risque de toucher au code Java.
- Pour appeler le backend, utilise toujours `NeneCareApi` (déjà dans `js/api.js`) :
  - `NeneCareApi.login(username, motDePasse)` → connexion
  - `NeneCareApi.logout()` → déconnexion
  - le jeton JWT est stocké automatiquement dans `sessionStorage`.
- Page d'accueil **selon le rôle** : lire le rôle renvoyé par le login
  (`reponse.donnees.role`) et afficher les menus correspondants.
- Les modifications du `static/` sont visibles en rechargeant la page (pas besoin de
  recompiler le Java si l'app tourne déjà... sinon relancer `./mvnw spring-boot:run`).

**QA / tests d'endpoints (curl)**
```bash
# Test du login
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","motDePasse":"test"}'

# Test du journal d'audit (ouvert pour la démo)
curl http://localhost:8081/api/audit/logs
```

---

### 🪪 Elimane — Dev Backend Auth & Autorisation

**Ce qui s'affiche par défaut**
Pas d'écran : tu testes via curl/Postman. L'endpoint `POST /api/auth/login` doit renvoyer
un JWT, `POST /api/auth/logout` doit révoquer la session.

**Où sont tes fichiers**
```
src/main/java/sn/esp/nenecare/
├── auth/
│   ├── controller/AuthController.java     → endpoints /api/auth/login et /logout
│   ├── service/AuthService.java           → logique de connexion (bcrypt → JWT)
│   ├── service/LoginAttemptService.java   → blocage après 5 échecs (US-04)
│   ├── jwt/JwtService.java                → génération / validation du JWT (30 min, US-02)
│   └── dto/LoginRequest.java, LoginResponse.java
└── common/                                → ApiResponse + gestion d'erreurs (partagé)
```

**Comment travailler**
- Toujours renvoyer `ApiResponse.ok(...)` / `ApiResponse.erreur(...)`.
- Enregistrer chaque connexion (réussie ou échouée) via `AuditService.logAction(...)`.
- Vérifier le mot de passe avec le `PasswordEncoder` (bcrypt) déjà déclaré dans `SecurityConfig`.

**Test rapide**
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"gyneco1","motDePasse":"secret"}'
```

---

### 🗂️ Amadou — Dev Backend Dossiers & Chiffrement

**État : fait.** Entités, chiffrement, DAC, liaison mère-enfant et endpoints REST
sont en place et testés (`patient/`, 19 tests dans `DossiersIT`).

**Où sont les fichiers**
```
src/main/java/sn/esp/nenecare/
├── patient/
│   ├── model/       Patiente, DossierMedical, DossierNeonatal (liaison @ManyToOne), StatutDossier
│   ├── repository/  requêtes filtrées par gynécologue référent (DAC)
│   ├── dto/         Request/Response par entité
│   ├── service/      PatienteService, DossierMedicalService, DossierNeonatalService,
│   │                 ControleAccesDossier (DAC + accès d'urgence US-14)
│   └── controller/  /api/patientes, /api/dossiers, /api/neonatals
├── crypto/
│   ├── AesGcmService.java, HmacService.java   → algorithmes
│   └── ProtectionDonneesService.java          → porte les clés, valide leur taille au démarrage
```

**Ce qui est appliqué**
- Chiffrement AES-256-GCM des champs médicaux **avant** `save`, déchiffrement après
  lecture — jamais de clair en base (vérifiable directement en SQL).
- Signature HMAC-SHA256 recalculée à chaque écriture, revérifiée à chaque lecture ;
  une entrée modifiée hors de l'application est signalée (`integre: false`) sans
  bloquer la consultation.
- **DAC** : un gynécologue ne voit que les patientes dont il est le référent
  (`findByGynecologueAssigneOrderByNomAsc` / filtre poussé en SQL).
- **Accès d'urgence (US-14)** : dérogation possible avec un motif obligatoire,
  toujours tracée dans le journal d'audit (`ACCES_URGENCE`).
- **Liaison mère-enfant (OS-06)** : `DossierNeonatal.mere` est `@ManyToOne(optional=false)`
  et fait partie du message signé — impossible de rattacher discrètement un
  dossier à une autre mère sans casser la signature.

---

### 🔐 Halima — Lead Sécurité & Architecte

**Ce qui s'affiche par défaut**
Pas d'écran : ton travail est transverse (sécurité + coordination).

**Où sont tes fichiers**
```
src/main/java/sn/esp/nenecare/
├── config/SecurityConfig.java   → configuration Spring Security (RBAC, filtres)
├── user/model/User.java         → entité utilisateur (rôle, bcrypt, actif/inactif)
├── user/model/Role.java         → les 6 rôles cliniques
└── auth/jwt/                    → y créer JwtAuthFilter.java
```

**Comment travailler**
- Créer `JwtAuthFilter.java` (lit `Authorization: Bearer ...`, valide via `JwtService`,
  place l'utilisateur dans le `SecurityContext`) et le **brancher** dans `SecurityConfig`.
- Poser le **RBAC** : annoter les endpoints de chacun avec
  `@PreAuthorize("hasRole('GYNECOLOGUE')")` (activé par `@EnableMethodSecurity`).
  ⚠️ Piège : l'authority doit être `ROLE_<NOM>` pour que `hasRole('NOM')` fonctionne.
- **Coordonner les merges** : relire et valider les Pull Requests vers `develop`.

---

## 4. Règle d'or de l'équipe

- On ne pousse **jamais** directement sur `main` ni `develop`.
- Chacun travaille sur sa branche `feature/*`, puis ouvre une **PR vers `develop`**.
- Détails complets : voir [CONTRIBUTING.md](../CONTRIBUTING.md) et [README.md](../README.md).
