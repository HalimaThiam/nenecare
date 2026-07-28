# Guide de contribution – NeneCare

Ce guide définit **comment l'équipe travaille ensemble** sur le dépôt pour éviter les
conflits et garder un historique propre. Lis-le avant de coder.

## 1. Modèle de branches (Git Flow)

| Branche | Rôle | Qui y pousse |
|---|---|---|
| `main` | Versions stables / démos | Personne en direct (merge depuis `develop` uniquement) |
| `develop` | Intégration du travail de tous | Personne en direct (merge via Pull Request) |
| `feature/*` | Travail en cours de chaque membre | Le propriétaire de la branche |

**Règle d'or : on ne commit JAMAIS directement sur `main` ni `develop`.**
Tout passe par une branche `feature/*` puis une Pull Request.

## 2. Répartition des branches

| Membre | Branche | Dossiers possédés |
|---|---|---|
| Elimane | `feature/auth` | `auth/`, `user/`, `common/` |
| Amadou | `feature/dossiers` | `patient/`, `crypto/`, `config/CryptoConfig` |
| Hadja | `feature/audit` | `audit/`, `resources/static/` |
| Halima | `feature/security` | `config/SecurityConfig`, revues, doc menaces |

Chacun travaille **dans ses dossiers** : ainsi les conflits sont quasi impossibles.
Les fichiers partagés (`SecurityConfig`, `user/User`, `user/Role`) se modifient **après
accord** sur le groupe WhatsApp, idéalement par Halima ou Elimane.

## 3. Démarrer le travail

```bash
# 1. Récupérer la dernière base d'intégration
git checkout develop
git pull origin develop

# 2. Créer SA branche de fonctionnalité (depuis develop)
git checkout -b feature/auth          # adapter le nom à son domaine

# 3. Coder, puis committer souvent avec des messages clairs
git add <fichiers>
git commit -m "auth: endpoint POST /api/auth/login (US-01)"

# 4. Pousser sa branche
git push -u origin feature/auth
```

## 4. Intégrer son travail (Pull Request)

1. Pousser sa branche `feature/*`.
2. Ouvrir une **Pull Request vers `develop`** sur GitHub (jamais vers `main`).
3. Demander **au moins une relecture** (Halima par défaut) avant le merge.
4. Une fois mergée, supprimer la branche distante si la fonctionnalité est terminée.

## 5. Se resynchroniser régulièrement

Avant de reprendre, récupérer le travail des autres pour éviter de diverger :

```bash
git checkout develop && git pull origin develop
git checkout feature/auth
git merge develop          # résoudre les éventuels conflits localement
```

## 6. Conventions de commit

- Messages en français, à l'impératif, **préfixés par le domaine** :
  - `auth:`, `crypto:`, `patient:`, `audit:`, `front:`, `config:`, `docs:`
- Référencer la user story quand c'est pertinent : `audit: filtrage par date (US-17)`.
- Petits commits cohérents plutôt qu'un gros commit fourre-tout.

## 7. À ne jamais committer

- Mots de passe, secrets, vraies clés cryptographiques (utiliser les variables
  d'environnement / `application-local.properties` qui est ignoré par git).
- Le dossier `target/`, les fichiers d'IDE (déjà dans `.gitignore`).

## 8. Avant d'ouvrir une PR

```bash
./mvnw test        # le build doit être VERT
```
