package com.adavec.transporte.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import com.adavec.transporte.dto.ApiError;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ResponseEntity<ApiError> buildResponseEntity(HttpStatus status, String message, String path) {
        ApiError error = new ApiError(
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return new ResponseEntity<>(error, status);
    }

    /**
     * Maneja errores de conversión de tipo en parámetros de método
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            WebRequest request) {

        String message;
        String path = request.getDescription(false).replace("uri=", "");

        // Manejo específico para parámetros de tipo YearMonth
        if (ex.getRequiredType() != null && ex.getRequiredType().equals(YearMonth.class)) {
            String ejemploFecha = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            message = String.format(
                    "Formato inválido para el parámetro '%s'. " +
                            "El formato esperado es YYYY-MM (ejemplo: %s). " +
                            "Valor recibido: '%s'",
                    ex.getName(),
                    ejemploFecha,
                    ex.getValue()
            );
        } else {
            message = String.format(
                    "Tipo de dato inválido para el parámetro '%s'. " +
                            "Se esperaba: %s, se recibió: '%s'",
                    ex.getName(),
                    ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "desconocido",
                    ex.getValue()
            );
        }

        logger.warn("Error de conversión de tipo: {}", message);
        return buildResponseEntity(HttpStatus.BAD_REQUEST, message, path);
    }

    /**
     * Maneja parámetros faltantes
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParams(
            MissingServletRequestParameterException ex,
            WebRequest request) {

        String message = String.format(
                "Parámetro requerido '%s' no está presente. Tipo esperado: %s",
                ex.getParameterName(),
                ex.getParameterType()
        );

        logger.warn("Parámetro faltante: {}", message);
        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                message,
                request.getDescription(false).replace("uri=", "")
        );
    }

    /**
     * Maneja argumentos ilegales (validaciones de negocio)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {

        logger.warn("Argumento ilegal: {}", ex.getMessage());
        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(
            ResponseStatusException ex,
            WebRequest request) {

        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return buildResponseEntity(
                status,
                ex.getReason() != null ? ex.getReason() : "Error en la solicitud",
                request.getDescription(false).replace("uri=", "")
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {

        logger.error("IllegalStateException: ", ex);
        return buildResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Estado ilegal: " + ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {

        logger.error("RuntimeException: ", ex);
        return buildResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor: " + ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllExceptions(
            Exception ex,
            WebRequest request) {

        logger.error("Excepción inesperada: ", ex);
        return buildResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error inesperado: " + ex.getMessage(),
                request.getDescription(false).replace("uri=", "")
        );
    }
}