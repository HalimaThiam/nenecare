// Helper d'appel a l'API REST NeneCare.
// Centralise le stockage du jeton JWT et les appels fetch.
// Proprietaire : Hadja (frontend). A enrichir au fur et a mesure.

const NeneCareApi = (() => {
    const CLE_TOKEN = 'nenecare_token';

    function setToken(token) {
        sessionStorage.setItem(CLE_TOKEN, token);
    }
    function getToken() {
        return sessionStorage.getItem(CLE_TOKEN);
    }
    function clearToken() {
        sessionStorage.removeItem(CLE_TOKEN);
    }

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
            throw new Error(corps.message || 'Erreur ' + reponse.status);
        }
        return corps;
    }

    function login(username, motDePasse) {
        return requete('/api/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, motDePasse })
        });
    }

    async function logout() {
        try {
            await requete('/api/auth/logout', { method: 'POST' });
        } finally {
            clearToken();
        }
    }

    return { setToken, getToken, clearToken, requete, login, logout };
})();
