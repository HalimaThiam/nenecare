package sn.esp.nenecare.config;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import sn.esp.nenecare.auth.service.NeneCareUserDetailsService;
import sn.esp.nenecare.patient.dto.DossierMedicalRequest;
import sn.esp.nenecare.patient.dto.DossierNeonatalRequest;
import sn.esp.nenecare.patient.dto.PatienteRequest;
import sn.esp.nenecare.patient.dto.PatienteResponse;
import sn.esp.nenecare.patient.repository.PatienteRepository;
import sn.esp.nenecare.patient.service.DossierMedicalService;
import sn.esp.nenecare.patient.service.DossierNeonatalService;
import sn.esp.nenecare.patient.service.PatienteService;
import sn.esp.nenecare.user.model.Role;

/**
 * Alimente la base de demonstration en patientes et dossiers.
 *
 * Les donnees passent par les VRAIS services metier, pas par les repositories.
 * C'est un choix deliberate : le jeu de demonstration est ainsi chiffre, signe
 * et audite exactement comme une saisie reelle. Inserer directement en base
 * produirait des dossiers non signes, qui s'afficheraient comme alteres a la
 * premiere consultation - et donneraient une demonstration trompeuse.
 *
 * Chaque creation est jouee sous l'identite du role qui en a le droit : la
 * secretaire enregistre les patientes, le gynecologue et la sage-femme
 * redigent les dossiers de suivi, le pediatre les dossiers neonatals. Cela
 * remplit du meme coup le journal d'audit avec des traces realistes.
 *
 * ACTIF UNIQUEMENT si nenecare.demo.enabled=true (profil dev).
 */
@Configuration
@ConditionalOnProperty(name = "nenecare.demo.enabled", havingValue = "true")
public class DossiersDemoInitializer {

    private static final Logger log = LoggerFactory.getLogger(DossiersDemoInitializer.class);

    private static final String IP_DEMO = "127.0.0.1";

    /** Ordre 20 : apres la creation des comptes (runner par defaut, ordre le plus bas). */
    @Bean
    @Order(20)
    public ApplicationRunner creerDossiersDemo(PatienteRepository patienteRepository,
                                               PatienteService patienteService,
                                               DossierMedicalService dossierMedicalService,
                                               DossierNeonatalService dossierNeonatalService) {
        return args -> {
            if (patienteRepository.count() > 0) {
                log.info("Dossiers de demonstration ignores : des patientes existent deja.");
                return;
            }

            try {
                // ── 1. Accueil : la secretaire enregistre les patientes ──────────
                sousIdentite("secretaire", Role.SECRETAIRE);

                PatienteResponse aissatou = patienteService.creer(patiente(
                        "NDIAYE", "Aissatou", LocalDate.of(1994, 3, 12),
                        "+221 77 512 44 08", "Ouakam, Cite Avion, Dakar",
                        "O+", "Aucun antecedent notable. Deuxieme grossesse.",
                        DonneesDemoInitializer.GYNECO_PRINCIPAL), IP_DEMO);

                PatienteResponse fatou = patienteService.creer(patiente(
                        "SARR", "Fatou", LocalDate.of(1989, 11, 2),
                        "+221 76 330 19 77", "Mermoz, Rue MZ-14, Dakar",
                        "A-", "Hypertension gravidique lors de la grossesse precedente.",
                        DonneesDemoInitializer.GYNECO_PRINCIPAL), IP_DEMO);

                PatienteResponse mariama = patienteService.creer(patiente(
                        "BA", "Mariama", LocalDate.of(1997, 6, 25),
                        "+221 70 884 22 31", "Yoff, Quartier Tonghor, Dakar",
                        "B+", "Diabete gestationnel suivi depuis le 2e trimestre.",
                        DonneesDemoInitializer.GYNECO_SECONDAIRE), IP_DEMO);

                patienteService.creer(patiente(
                        "FALL", "Khady", LocalDate.of(1992, 1, 18),
                        "+221 78 201 65 40", "Ngor, Village, Dakar",
                        "AB+", "Primipare. Suivi prenatal regulier.",
                        DonneesDemoInitializer.GYNECO_SECONDAIRE), IP_DEMO);

                // ── 2. Suivi de grossesse : le gynecologue referent ──────────────
                sousIdentite(DonneesDemoInitializer.GYNECO_PRINCIPAL, Role.GYNECOLOGUE);

                dossierMedicalService.creer(dossier(aissatou.getId(),
                        LocalDate.now().minusDays(12), "Consultation prenatale - 32 SA",
                        "Grossesse evolutive, presentation cephalique. Hauteur uterine 30 cm.",
                        "Fer + acide folique, 1 comprime par jour jusqu'au terme.",
                        "Tension 11/7. Rendez-vous de controle dans 3 semaines."), IP_DEMO);

                dossierMedicalService.creer(dossier(fatou.getId(),
                        LocalDate.now().minusDays(4), "Consultation prenatale - 28 SA",
                        "Tension arterielle a 14/9, surveillance rapprochee necessaire.",
                        "Alpha-methyldopa 250 mg matin et soir. Repos strict.",
                        "Bilan renal demande. Reevaluation sous 7 jours."), IP_DEMO);

                // ── 3. La sage-femme intervient sur tout le service ──────────────
                sousIdentite("sagefemme", Role.SAGE_FEMME);

                dossierMedicalService.creer(dossier(mariama.getId(),
                        LocalDate.now().minusDays(2), "Suivi de diabete gestationnel",
                        "Glycemie post-prandiale a 1,52 g/L. Equilibre insuffisant.",
                        "Adaptation du regime, autosurveillance glycemique 4 fois par jour.",
                        "Consultation dietetique programmee."), IP_DEMO);

                // ── 4. Nouveau-nes : le pediatre (liaison mere-enfant, OS-06) ────
                sousIdentite("pediatre", Role.PEDIATRE);

                dossierNeonatalService.creer(neonatal(aissatou.getId(),
                        "Ousmane NDIAYE", "M", LocalDate.now().minusDays(9),
                        3240, 49, 9,
                        "Nouveau-ne a terme, examen clinique normal.",
                        "Allaitement maternel exclusif mis en place. Vitamine K administree."),
                        IP_DEMO);

                dossierNeonatalService.creer(neonatal(fatou.getId(),
                        "Adama SARR", "F", LocalDate.now().minusDays(3),
                        2680, 46, 8,
                        "Petit poids de naissance lie a l'hypertension maternelle.",
                        "Surveillance de la courbe ponderale. Controle a 48 heures."),
                        IP_DEMO);

                log.warn("=================================================================");
                log.warn(" DOSSIERS DE DEMONSTRATION CREES");
                log.warn("   4 patientes, 3 dossiers de suivi, 2 dossiers neonatals");
                log.warn("   Contenu medical chiffre AES-256-GCM et signe HMAC-SHA256.");
                log.warn("=================================================================");

            } finally {
                // Le contexte de securite est porte par le thread : le laisser
                // rempli contaminerait la premiere requete traitee par ce thread.
                SecurityContextHolder.clearContext();
            }
        };
    }

    /**
     * Rejoue une identite le temps d'une serie de creations.
     *
     * Les services lisent l'appelant dans le SecurityContext : sans cela, les
     * dossiers seraient crees par "anonyme" et le controle d'acces
     * discretionnaire ne pourrait pas etre demontre.
     */
    private void sousIdentite(String username, Role role) {
        var authentification = new UsernamePasswordAuthenticationToken(
                username, null,
                List.of(new SimpleGrantedAuthority(
                        NeneCareUserDetailsService.PREFIXE_ROLE + role.name())));
        SecurityContextHolder.getContext().setAuthentication(authentification);
    }

    // -------------------------------------------------------------------------
    // Fabriques de requetes
    // -------------------------------------------------------------------------

    private PatienteRequest patiente(String nom, String prenom, LocalDate naissance,
                                     String telephone, String adresse, String groupeSanguin,
                                     String antecedents, String gynecologue) {
        PatienteRequest requete = new PatienteRequest();
        requete.setNom(nom);
        requete.setPrenom(prenom);
        requete.setDateNaissance(naissance);
        requete.setTelephone(telephone);
        requete.setAdresse(adresse);
        requete.setGroupeSanguin(groupeSanguin);
        requete.setAntecedents(antecedents);
        requete.setGynecologueAssigne(gynecologue);
        return requete;
    }

    private DossierMedicalRequest dossier(Long patienteId, LocalDate date, String motif,
                                          String diagnostic, String traitement,
                                          String observations) {
        DossierMedicalRequest requete = new DossierMedicalRequest();
        requete.setPatienteId(patienteId);
        requete.setDateConsultation(date);
        requete.setMotif(motif);
        requete.setDiagnostic(diagnostic);
        requete.setTraitement(traitement);
        requete.setObservations(observations);
        return requete;
    }

    private DossierNeonatalRequest neonatal(Long mereId, String nomBebe, String sexe,
                                            LocalDate naissance, Integer poids, Integer taille,
                                            Integer apgar, String diagnostic,
                                            String observations) {
        DossierNeonatalRequest requete = new DossierNeonatalRequest();
        requete.setMereId(mereId);
        requete.setNomBebe(nomBebe);
        requete.setSexe(sexe);
        requete.setDateNaissance(naissance);
        requete.setPoidsGrammes(poids);
        requete.setTailleCm(taille);
        requete.setScoreApgar(apgar);
        requete.setDiagnostic(diagnostic);
        requete.setObservations(observations);
        return requete;
    }
}
