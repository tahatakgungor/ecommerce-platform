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

    // Senin mevcut kullandığın alan
    private T data;

    // Harri şablonunun (Next.js) beklediği alan
    // JsonProperty sayesinde JSON çıktısında "result" olarak görünür
    @JsonProperty("result")
    public T getResult() {
        return data;
    }

    private Long total;

    // Kolaylık sağlaması için constructor (eğer total göndermeyeceksen)
    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.total = data instanceof java.util.Collection ? (long) ((java.util.Collection<?>) data).size() : 1L;
    }
}