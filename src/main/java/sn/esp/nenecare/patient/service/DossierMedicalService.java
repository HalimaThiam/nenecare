package sn.esp.nenecare.patient.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sn.esp.nenecare.patient.repository.DossierMedicalRepository;

/**
 * Gestion des dossiers medicaux (US-05 a US-13).
 * STUB de base - a completer par Amadou.
 *
 * Doit utiliser AesGcmService (chiffrement) et HmacService (integrite)
 * du package crypto avant/apres persistance.
 */
@Service
@RequiredArgsConstructor
public class DossierMedicalService {

    private final DossierMedicalRepository dossierMedicalRepository;

    // TODO US-07/08 : creer, lire (avec dechiffrement + verif HMAC), modifier un dossier.
    // TODO US-09 : archiver un dossier.
}
