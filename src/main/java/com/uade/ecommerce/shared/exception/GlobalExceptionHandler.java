package com.uade.ecommerce.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
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
 *
 * <p>También entran acá los errores de Spring Security (ítem 19). Esos no los tira un controller
 * sino la cadena de filtros, que corre antes del DispatcherServlet, así que por sí solos nunca
 * llegarían a un @RestControllerAdvice: se devolverían con el HTML/vacío por defecto de Spring
 * Security y el cliente recibiría dos formatos de error distintos según dónde falló. Por eso
 * {@code SecurityConfig} redirige su authenticationEntryPoint y su accessDeniedHandler al
 * HandlerExceptionResolver, que los trae hasta los dos handlers de abajo.</p>
 *
 * <p>Ítems 30 y 31: no hizo falta inventar excepciones nuevas. "No autenticado" y "token
 * inválido" ya los cubren las AuthenticationException de Spring Security; "sin permiso" sigue
 * siendo ForbiddenException (403); y para roles se reutilizan ResourceNotFoundException y
 * DuplicateResourceException, que no son específicas de ninguna entidad.</p>
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

    // 401 - Login con email o contraseña incorrectos
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> handleCredencialesInvalidas(
            CredencialesInvalidasException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
    }

    // 403 - El recurso existe pero quien lo pide no es su dueño
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException exception,
            HttpServletRequest request
    ) {
        return build(HttpStatus.FORBIDDEN, exception.getMessage(), request);
    }

    /**
     * 401 - No hay credenciales, o el token es inválido o venció.
     *
     * <p>El mensaje es genérico a propósito: decir "el token venció" o "ese usuario no existe"
     * le da información gratis a quien está probando.</p>
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleNoAutenticado(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.UNAUTHORIZED,
                "Necesitás iniciar sesión para acceder a este recurso",
                request
        );
    }

    /**
     * 403 - El usuario está autenticado pero su rol no alcanza para esta operación.
     *
     * <p>Es el hermano de ForbiddenException: aquella la lanza el código cuando el recurso es de
     * otro usuario, y esta la lanza Spring Security cuando la ruta pide un rol que no se tiene.</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return build(
                HttpStatus.FORBIDDEN,
                "No tenés permiso para realizar esta operación",
                request
        );
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
