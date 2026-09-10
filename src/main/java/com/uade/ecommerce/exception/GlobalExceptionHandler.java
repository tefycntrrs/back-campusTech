package com.uade.ecommerce.exception;

import com.uade.ecommerce.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo global de excepciones: ningún controller arma respuestas de error,
 * todas pasan por acá y salen con el mismo formato (ErrorResponse).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 - Cubre Producto, Categoria, Marca, Usuario, Pedido, etc.
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    // 400 - Reglas de negocio / argumentos inválidos lanzados a mano
    @ExceptionHandler(ArgumentInvalidException.class)
    public ResponseEntity<ErrorResponse> handleArgumentInvalid(
            ArgumentInvalidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errores = null;

        if (exception.getCampo() != null) {
            errores = Map.of(exception.getCampo(), exception.getMessage());
        }

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                errores
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 403 - El recurso existe pero quien lo pide no es su dueño
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    // 409 - Valores únicos repetidos (email, sku, nombre)
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    // 400 - Validaciones declarativas (@Valid sobre los DTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> errores = new LinkedHashMap<>();

        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );

        ErrorResponse body = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Hay campos inválidos en la petición",
                request.getRequestURI(),
                errores
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 400 - JSON mal formado o valor de enum inexistente (por ejemplo sexo)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición es inválido: " + causaLegible(exception),
                request
        );
    }

    // 400 - Path variable o query param con tipo equivocado (/api/productos/abc)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.BAD_REQUEST,
                "El parámetro '" + exception.getName() + "' tiene un valor inválido: " + exception.getValue(),
                request
        );
    }

    // Se mantiene por compatibilidad con el código que todavía usa ResponseStatusException
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String mensaje = exception.getReason() != null ? exception.getReason() : status.getReasonPhrase();

        return build(status, mensaje, request);
    }

    // 500 - Red de contención para lo que no esté contemplado arriba
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado: " + exception.getMessage(),
                request
        );
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status,
            String mensaje,
            HttpServletRequest request
    ) {
        ErrorResponse body = ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                mensaje,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }

    private String causaLegible(HttpMessageNotReadableException exception) {
        Throwable causa = exception.getMostSpecificCause();
        String mensaje = causa.getMessage();

        if (mensaje == null) {
            return "revisá el formato del JSON";
        }

        // Los mensajes de Jackson traen el stacktrace del parser en la segunda línea
        return mensaje.split("\n")[0];
    }
}
