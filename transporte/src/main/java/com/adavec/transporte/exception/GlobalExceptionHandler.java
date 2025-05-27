package com.adavec.transporte.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import com.adavec.transporte.dto.ApiError;
import com.adavec.transporte.exception.DuplicateEntityException;
import com.adavec.transporte.exception.BusinessValidationException;
import com.adavec.transporte.exception.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
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
     * Maneja errores de validación Bean Validation (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationErrors(
            MethodArgumentNotValidException ex,
            WebRequest request) {

        StringBuilder errores = new StringBuilder("Errores de validación: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errores.append(error.getField())
                    .append(": ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        });

        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("Errores de validación: {}", errores.toString());

        return buildResponseEntity(HttpStatus.BAD_REQUEST, errores.toString(), path);
    }

    /**
     * Maneja violaciones de restricciones de validación
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        StringBuilder errores = new StringBuilder("Violaciones de restricción: ");
        ex.getConstraintViolations().forEach(violation -> {
            errores.append(violation.getPropertyPath())
                    .append(": ")
                    .append(violation.getMessage())
                    .append("; ");
        });

        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("Violación de restricciones: {}", errores.toString());

        return buildResponseEntity(HttpStatus.BAD_REQUEST, errores.toString(), path);
    }

    /**
     * Maneja errores de integridad de datos (duplicados, FK, etc.)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            WebRequest request) {

        String message = "Error de integridad de datos";

        // Detectar tipos específicos de errores
        String errorMsg = ex.getMessage();
        if (errorMsg != null) {
            if (errorMsg.contains("Duplicate entry") || errorMsg.contains("duplicate key")) {
                message = "Ya existe un registro con esos datos";
            } else if (errorMsg.contains("foreign key constraint")) {
                message = "Error de referencia: entidad relacionada no existe o está siendo utilizada";
            } else if (errorMsg.contains("cannot be null")) {
                message = "Campo requerido no puede estar vacío";
            }
        }

        String path = request.getDescription(false).replace("uri=", "");
        logger.error("Error de integridad de datos: ", ex);

        return buildResponseEntity(HttpStatus.CONFLICT, message, path);
    }

    /**
     * Maneja entidades no encontradas (personalizada)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("Entidad no encontrada: {}", ex.getMessage());

        return buildResponseEntity(
                HttpStatus.NOT_FOUND,
                ex.getMessage() != null ? ex.getMessage() : "Entidad no encontrada",
                path
        );
    }

    /**
     * Maneja entidades duplicadas
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ApiError> handleDuplicateEntityException(
            DuplicateEntityException ex,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("Entidad duplicada: {}", ex.getMessage());

        return buildResponseEntity(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                path
        );
    }

    /**
     * Maneja validaciones de negocio
     */
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiError> handleBusinessValidationException(
            BusinessValidationException ex,
            WebRequest request) {

        String path = request.getDescription(false).replace("uri=", "");
        logger.warn("Error de validación de negocio: {}", ex.getMessage());

        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                path
        );
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