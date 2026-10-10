package com.LaFerreteria.LaFerreteria.controller.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(
        basePackages = "com.LaFerreteria.LaFerreteria.controller.api"
)
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> datosInvalidos(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest().body(
                Map.of("mensaje", exception.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacionInvalida(
            MethodArgumentNotValidException exception
    ) {
        String mensaje = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Revise los datos ingresados");

        return ResponseEntity.badRequest().body(
                Map.of("mensaje", mensaje)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido() {
        return ResponseEntity.badRequest().body(
                Map.of("mensaje", "Los datos enviados no son validos")
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> datoDuplicado() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("mensaje", "Ya existe un registro con esos datos")
        );
    }
}
