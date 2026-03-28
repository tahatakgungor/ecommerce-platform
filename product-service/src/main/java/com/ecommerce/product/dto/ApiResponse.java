package com.ecommerce.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Long total;

    @JsonProperty("result")
    public T getResult() {
        return data;
    }

    // 1. ESKİ KODLAR İÇİN (Brand, Product vb.): 3 Parametreli
    public ApiResponse(boolean success, T data, Long total) {
        this.success = success;
        this.data = data;
        this.total = total;
        this.message = success ? "Success" : "Error";
    }

    // 2. HATALAR VE MESAJLAR İÇİN: Sadece Mesaj dönen statik metodlar (Çakışmayı önler)
    // Constructor yerine bunları kullanmak "Related Problems" hatasını bitirir.
    public static <T> ApiResponse<T> ok(T data, Long total) {
        return new ApiResponse<>(true, data, total);
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setTotal(0L);
        return response;
    }
}