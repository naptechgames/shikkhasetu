package bd.bubt.shikkhasetu.web;

import java.util.Map;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Turns exceptions into a small JSON answer: {"error": "..."} with the right HTTP status. */
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApi(ApiException e) {
        return answer(e.getStatus(), e.getMessage());
    }

    /** A required field is missing or has a wrong value. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst().orElse("Invalid input");
        return answer(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Map<String, String>> handleUnreadable(Exception e) {
        return answer(HttpStatus.BAD_REQUEST, "Invalid input");
    }

    /** Two users changed the same row at the same time (lock or version conflict). */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<Map<String, String>> handleConcurrency(ConcurrencyFailureException e) {
        return answer(HttpStatus.CONFLICT, "Someone else changed this item just now. Please refresh.");
    }

    private ResponseEntity<Map<String, String>> answer(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
