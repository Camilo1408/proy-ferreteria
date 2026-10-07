/*
 * nombre: GlobalExceptionHandler.java
 * descripcion: Traduce excepciones a respuestas JSON uniformes e internacionalizadas.
 * fecha_creacion: 2026-10-07
 * actualizacion: 2026-10-07
 * autor: Camilo1408
 * version: 1.0.0
 */
package com.ferreteria.error;

import com.ferreteria.producto.NombreDuplicadoException;
import com.ferreteria.producto.ProductoNoEncontradoException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Manejador global de errores de la API. */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messages;

    private ResponseEntity<ErrorResponse> resp(HttpStatus st, String codigo, String clave, List<String> det,
                                               Object... args) {
        String mensaje = messages.getMessage(clave, args, clave, LocaleContextHolder.getLocale());
        return ResponseEntity.status(st).body(new ErrorResponse(codigo, mensaje, det, Instant.now()));
    }

    /** 400: fallos de validación de campos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        List<String> det = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).sorted().toList();
        return resp(HttpStatus.BAD_REQUEST, "VALIDACION", "error.validacion", det);
    }

    /** 400: JSON malformado, parámetros faltantes o con tipo inválido. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> solicitudInvalida(Exception ex) {
        return resp(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", "error.solicitud", List.of());
    }

    /** Regla de negocio violada: estado, código y mensaje los define quien la lanza. */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> negocio(NegocioException ex) {
        return resp(ex.getStatus(), ex.getCodigo(), ex.getClave(), List.of(), ex.getArgs());
    }

    /** 409: violación de unicidad u otra restricción de la base (por ejemplo, carreras al crear). */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integridad(org.springframework.dao.DataIntegrityViolationException ex) {
        return resp(HttpStatus.CONFLICT, "CONFLICTO_DATOS", "error.integridad", List.of());
    }

    /** 404: producto inexistente. */
    @ExceptionHandler(ProductoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(ProductoNoEncontradoException ex) {
        return resp(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", "error.noEncontrado", List.of(), ex.getMessage());
    }

    /** 409: nombre duplicado. */
    @ExceptionHandler(NombreDuplicadoException.class)
    public ResponseEntity<ErrorResponse> duplicado(NombreDuplicadoException ex) {
        return resp(HttpStatus.CONFLICT, "NOMBRE_DUPLICADO", "error.duplicado", List.of(), ex.getMessage());
    }

    /** 401: credenciales de login incorrectas. */
    @ExceptionHandler(com.ferreteria.security.CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> credenciales(com.ferreteria.security.CredencialesInvalidasException ex) {
        return resp(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", "error.credenciales", List.of());
    }

    /** 403: rol insuficiente. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> prohibido(AccessDeniedException ex) {
        return resp(HttpStatus.FORBIDDEN, "PROHIBIDO", "error.prohibido", List.of());
    }

    /** 404: ruta inexistente. */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> rutaInexistente(Exception ex) {
        return resp(HttpStatus.NOT_FOUND, "RUTA_NO_ENCONTRADA", "error.ruta", List.of());
    }

    /** 405: método HTTP no permitido en la ruta. */
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> metodoNoPermitido(Exception ex) {
        return resp(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO", "error.metodo", List.of());
    }

    /** 415: tipo de contenido no soportado. */
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> tipoNoSoportado(Exception ex) {
        return resp(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "TIPO_NO_SOPORTADO", "error.tipoContenido", List.of());
    }

    /** 500: cualquier error no previsto, sin exponer detalles internos. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> interno(Exception ex) {
        return resp(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "error.interno", List.of());
    }
}
