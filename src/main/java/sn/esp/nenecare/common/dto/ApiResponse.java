package sn.esp.nenecare.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Enveloppe de reponse standard pour toute l'API NeneCare.
 * Permet a tous les membres de renvoyer un format homogene au frontend.
 */
@Data
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean succes;
    private String message;
    private T donnees;

    public static <T> ApiResponse<T> ok(T donnees) {
        return new ApiResponse<>(true, "OK", donnees);
    }

    public static <T> ApiResponse<T> ok(String message, T donnees) {
        return new ApiResponse<>(true, message, donnees);
    }

    public static <T> ApiResponse<T> erreur(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
