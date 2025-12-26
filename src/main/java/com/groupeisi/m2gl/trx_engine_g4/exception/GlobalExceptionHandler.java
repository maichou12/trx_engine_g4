package com.groupeisi.m2gl.trx_engine_g4.exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur de validation des données",
                errors
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 2. Gestion des ressources non trouvées (404)
// Utilisé lorsque le compte émetteur ou récepteur est introuvable (levé par le Service).
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleEntityNotFound(EntityNotFoundException ex) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Compte introuvable",
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // 3. Gestion des erreurs métier (Solde insuffisant, comptes identiques, compte bloqué)
    // Ces erreurs sont levées explicitement dans le TransfertService (IllegalArgumentException/IllegalStateException).
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> handleBusinessLogicErrors(RuntimeException ex) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur métier",
                ex.getMessage()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleJsonParsingError(HttpMessageNotReadableException ex) {
        String errorMessage = "Erreur de format JSON. Vérifiez la syntaxe (ex: UUID, types de données).";

        // Si l'erreur concerne un UUID malformé, on peut donner plus de détails
        if (ex.getMessage() != null && ex.getMessage().contains("UUID has to be represented by standard 36-char representation")) {
            errorMessage = "Erreur de format UUID. L'identifiant de compte doit être au format 36 caractères standard.";
        }

        ApiResponse errorResponse = new ApiResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST.value(),
                false
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // 4. Gestion globale (Erreurs inattendues - 500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGlobalException(Exception ex) {
        // En production, il est crucial de logguer l'erreur complète (ex)
        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erreur interne du serveur",
                "Une erreur inattendue s'est produite. Veuillez contacter l'administrateur."
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


