package sn.esp.nenecare.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import sn.esp.nenecare.config.ApplicationContextProvider;

/**
 * Convertisseur JPA qui chiffre/déchiffre les champs String transparemment.
 *
 * Utilisation sur un champ d'entité :
 *   @Convert(converter = EncryptedStringConverter.class)
 *   @Column(columnDefinition = "TEXT")
 *   private String champSensible;
 *
 * Hibernate instancie ce converter en dehors du contexte Spring — l'encrypteur
 * est récupéré via ApplicationContextProvider (initialisé avant JPA au démarrage).
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private AesGcmEncryptor encryptor() {
        return ApplicationContextProvider.getBean(AesGcmEncryptor.class);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) return null;
        return encryptor().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        return encryptor().decrypt(dbData);
    }
}
