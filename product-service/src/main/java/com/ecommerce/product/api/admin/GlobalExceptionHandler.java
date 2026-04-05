package com.ecommerce.product.api.admin;

import com.ecommerce.product.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.NoSuchFileException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<String> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Yetki hatası: {}", ex.getMessage());
        return ApiResponse.error("Bu işlem için yetkiniz bulunmuyor.");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<String> handleAuthentication(AuthenticationException ex) {
        log.warn("Kimlik doğrulama hatası: {}", ex.getMessage());
        return ApiResponse.error("Oturum doğrulanamadı. Lütfen tekrar giriş yapın.");
    }

    @ExceptionHandler({NoResourceFoundException.class, NoSuchFileException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<String> handleMissingResource(Exception ex) {
        log.warn("Kaynak bulunamadı: {}", ex.getMessage());
        return ApiResponse.error("Kaynak bulunamadı.");
    }

    // RuntimeException önce yakalanmalı (daha spesifik)
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleRuntimeExceptions(RuntimeException ex) {
        log.warn("İş mantığı hatası: {}", ex.getMessage(), ex);
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
        return ApiResponse.error(message);
    }

    // Genel Exception en sona
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleAllExceptions(Exception ex) {
        log.error("Beklenmedik bir hata oluştu: ", ex);
        return ApiResponse.error("Sunucu hatası: " + ex.getMessage());
    }
}
