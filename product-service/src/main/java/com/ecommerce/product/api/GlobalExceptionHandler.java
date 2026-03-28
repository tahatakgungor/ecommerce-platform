package com.ecommerce.product.api;

import com.ecommerce.product.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<String> handleAllExceptions(Exception ex) {
        log.error("Beklenmedik bir hata oluştu: ", ex);
        // ApiResponse içindeki static error metodunu kullanıyoruz
        return ApiResponse.error("Sunucu hatası: " + ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<String> handleRuntimeExceptions(RuntimeException ex) {
        // Hata mesajını doğrudan error metoduyla dönüyoruz
        return ApiResponse.error(ex.getMessage());
    }
}