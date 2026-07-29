// =============================================================================
// Helper d'appel a l'API REST NeneCare.
// Centralise le stockage du jeton JWT, les appels fetch et la garde d'acces.
// Proprietaire : Hadja (frontend).
//
// Le jeton est place dans sessionStorage : il disparait a la fermeture de
// l'onglet, contrairement a localStorage qui le laisserait trainer sur un
// poste partage de la clinique.
// =============================================================================

const NeneCareApi = (() => {

    const CLE_TOKEN       = 'nenecare_token';
    const CLE_UTILISATEUR = 'nenecare_utilisateur';

    // -------------------------------------------------------------------------
    // Session
    // -------------------------------------------------------------------------

    function setSession(donnees) {
        sessionStorage.setItem(CLE_TOKEN, donnees.token);
        sessionStorage.setItem(CLE_UTILISATEUR, JSON.stringify({
            username:   donnees.username,
            nomComplet: donnees.nomComplet,
            role:       donnees.role
        }));
    }

    function getToken() {
        return sessionStorage.getItem(CLE_TOKEN);
    }

    function getUtilisateur() {
        const brut = sessionStorage.getItem(CLE_UTILISATEUR);
        return brut ? JSON.parse(brut) : null;
    }

    function estConnecte() {
        return getToken() !== null;
    }

    function aLeRole(role) {
        const utilisateur = getUtilisateur();
        return utilisateur !== null && utilisateur.role === role;
    }

    function viderSession() {
        sessionStorage.removeItem(CLE_TOKEN);
        sessionStorage.removeItem(CLE_UTILISATEUR);
    }

    /**
     * Redirige vers la page de connexion si aucune session n'est ouverte.
     * Confort d'affichage uniquement : la vraie barriere est cote serveur.
     */
    function exigerConnexion() {
        if (!estConnecte()) {
            window.location.replace('/login.html');
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Appels HTTP
    // -------------------------------------------------------------------------

    async function requete(url, options = {}) {
        const headers = Object.assign(
            { 'Content-Type': 'application/json' },
            options.headers || {}
        );

        const token = getToken();
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }

        const reponse = await fetch(url, Object.assign({}, options, { headers }));
        const corps = await reponse.json().catch(() => ({}));

        if (!reponse.ok) {
            // Jeton expire (US-02) ou revoque : on renvoie l'utilisateur au login.
            if (reponse.status === 401 && estConnecte()) {
                viderSession();
                window.location.replace('/login.html?expire=1');
            }
            const erreur = new Error(corps.message || 'Erreur ' + reponse.status);
            erreur.statut = reponse.status;
            throw erreur;
        }

        // Les reponses metier sont enveloppees dans ApiResponse : on rend
        // directement le contenu utile quand c'est le cas.
        return (corps && 'donnees' in corps) ? corps.donnees : corps;
    }

    function get(url) {
        return requete(url, { method: 'GET' });
    }

    // -------------------------------------------------------------------------
    // Authentification
    // -------------------------------------------------------------------------

    async function login(username, motDePasse) {
        const donnees = await requete('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, motDePasse })
        });
        setSession(donnees);
        return donnees;
    }

    async function logout() {
        try {
            await requete('/api/auth/logout', { method: 'POST' });
        } catch (e) {
            // La deconnexion locale doit aboutir meme si le serveur est injoignable.
        } finally {
            viderSession();
        }
    }

    return {
        getToken, getUtilisateur, estConnecte, aLeRole,
        exigerConnexion, viderSession,
        requete, get, login, logout
    };
})();
