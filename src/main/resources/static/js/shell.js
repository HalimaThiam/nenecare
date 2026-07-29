// =============================================================================
// Coquille commune aux pages authentifiees : barre laterale, identite de la
// personne connectee, navigation adaptee au role, deconnexion.
// Proprietaire : Hadja (frontend).
//
// RAPPEL : masquer un onglet est du confort d'affichage. Le controle d'acces
// reel est cote serveur (JwtAuthFilter + @PreAuthorize) ; l'API repond 403
// meme si quelqu'un force l'URL a la main.
// =============================================================================

const NeneCareShell = (() => {

    const ICONES = {
        tableau:  '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/>',
        patiente: '<path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
        dossier:  '<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>',
        neonatal: '<path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>',
        audit:    '<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>',
        acces:    '<circle cx="12" cy="12" r="3"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/><path d="M4.93 4.93a10 10 0 0 0 0 14.14"/>',
        coeur:    '<path d="M12 21.593c-5.63-5.539-11-10.297-11-14.402 0-3.791 3.068-5.191 5.281-5.191 1.312 0 4.151.501 5.719 4.457 1.59-3.968 4.464-4.447 5.726-4.447 2.54 0 5.274 1.621 5.274 5.181 0 4.069-5.136 8.625-11 14.402z"/>',
        sortie:   '<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>'
    };

    /**
     * Onglets et roles qui y ont acces cote serveur.
     * Ces listes DOIVENT rester alignees sur les @PreAuthorize des controleurs,
     * sinon on propose a l'utilisateur des pages qui lui repondront 403.
     */
    const NAVIGATION = [
        { id: 'tableau',  libelle: 'Tableau de bord',    lien: '/accueil.html',           icone: 'tableau',
          roles: ['ADMIN', 'GYNECOLOGUE', 'PEDIATRE', 'SAGE_FEMME', 'INFIRMIER', 'SECRETAIRE'] },
        { id: 'patientes', libelle: 'Patientes',         lien: '/dossiers.html#patientes', icone: 'patiente',
          roles: ['ADMIN', 'GYNECOLOGUE', 'PEDIATRE', 'SAGE_FEMME', 'INFIRMIER', 'SECRETAIRE'] },
        { id: 'dossiers',  libelle: 'Dossiers médicaux', lien: '/dossiers.html#dossiers',  icone: 'dossier',
          roles: ['ADMIN', 'GYNECOLOGUE', 'SAGE_FEMME', 'INFIRMIER'] },
        { id: 'neonatals', libelle: 'Dossiers néonatals', lien: '/dossiers.html#neonatals', icone: 'neonatal',
          roles: ['ADMIN', 'PEDIATRE', 'SAGE_FEMME', 'INFIRMIER'] }
    ];

    const NAVIGATION_ADMIN = [
        { id: 'audit', libelle: "Journal d'audit", lien: '/audit.html', icone: 'audit', roles: ['ADMIN'] }
    ];

    function svg(nom, taille) {
        return '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"'
             + (taille ? ' style="width:' + taille + ';height:' + taille + ';"' : '')
             + '>' + ICONES[nom] + '</svg>';
    }

    function lienNav(entree, actif) {
        return '<a class="nav-item' + (actif === entree.id ? ' active' : '') + '" href="'
             + entree.lien + '">' + svg(entree.icone) + entree.libelle + '</a>';
    }

    /**
     * Construit la barre laterale dans l'element #sidebar.
     * @param actif identifiant de l'onglet a mettre en evidence
     */
    function monter(actif) {
        if (!NeneCareApi.exigerConnexion()) {
            return null;
        }

        const utilisateur = NeneCareApi.getUtilisateur();
        const role = utilisateur.role;
        const nomAffiche = utilisateur.nomComplet || utilisateur.username;
        const initiales = nomAffiche.split(/\s+/).filter(Boolean).slice(0, 2)
                                    .map(mot => mot[0].toUpperCase()).join('');

        const onglets = NAVIGATION.filter(e => e.roles.includes(role))
                                  .map(e => lienNav(e, actif)).join('');

        const ongletsAdmin = NAVIGATION_ADMIN.filter(e => e.roles.includes(role));
        const blocAdmin = ongletsAdmin.length === 0 ? '' :
              '<div class="nav-section-label" style="margin-top:1.25rem;">Administration</div>'
            + ongletsAdmin.map(e => lienNav(e, actif)).join('');

        document.getElementById('sidebar').innerHTML = `
          <div class="sidebar-logo">
            <div class="sidebar-logo-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2"
                   stroke-linecap="round">${ICONES.coeur}</svg>
            </div>
            <span class="sidebar-logo-name">NeneCare</span>
          </div>
          <div class="nav-section-label">Navigation</div>
          ${onglets}
          ${blocAdmin}
          <div class="nav-spacer"></div>
          <div class="sidebar-user">
            <div class="user-avatar">${initiales}</div>
            <div class="user-info">
              <div class="user-name"></div>
              <div class="user-role"></div>
            </div>
            <button class="logout-btn" id="btnDeconnexion" title="Déconnexion">
              ${svg('sortie')}
            </button>
          </div>`;

        // textContent et pas innerHTML : le nom vient de la base, il ne doit
        // jamais pouvoir etre interprete comme du HTML.
        document.querySelector('.sidebar .user-name').textContent = nomAffiche;
        document.querySelector('.sidebar .user-role').textContent = role;

        document.getElementById('btnDeconnexion').addEventListener('click', async () => {
            await NeneCareApi.logout();
            window.location.replace('/login.html');
        });

        return utilisateur;
    }

    /** Vrai si le role connecte figure dans la liste donnee. */
    function peut(roles) {
        const utilisateur = NeneCareApi.getUtilisateur();
        return utilisateur !== null && roles.includes(utilisateur.role);
    }

    // -------------------------------------------------------------------------
    // Fabriques DOM sures
    // -------------------------------------------------------------------------

    /** Cree un element en passant par textContent : aucune injection possible. */
    function el(balise, classe, texte) {
        const element = document.createElement(balise);
        if (classe) element.className = classe;
        if (texte !== undefined && texte !== null) element.textContent = texte;
        return element;
    }

    function badge(texte, type) {
        return el('span', 'badge ' + (type || 'neutre'), texte);
    }

    function vide(message) {
        const bloc = el('div', 'vide');
        bloc.appendChild(el('div', null, message));
        return bloc;
    }

    function date(valeur) {
        if (!valeur) return '—';
        const d = new Date(valeur);
        return isNaN(d) ? '—' : d.toLocaleDateString('fr-FR');
    }

    function dateHeure(valeur) {
        if (!valeur) return '—';
        const d = new Date(valeur);
        return isNaN(d) ? '—' : d.toLocaleString('fr-FR');
    }

    return { monter, peut, el, badge, vide, date, dateHeure, svg, ICONES };
})();
