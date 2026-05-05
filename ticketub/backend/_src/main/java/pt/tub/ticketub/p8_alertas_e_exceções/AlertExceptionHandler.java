package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class AlertExceptionHandler {

    @ExceptionHandler(AlertNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(AlertNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AlertValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(AlertValidationException exception, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBeanValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, details.toString(), request.getRequestURI());
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String path) {
        ApiErrorResponse body = new ApiErrorResponse(
            OffsetDateTime.now(ZoneOffset.UTC),
            status.value(),
            status.getReasonPhrase(),
            message,
            path
        );
        return ResponseEntity.status(status).body(body);
    }
}