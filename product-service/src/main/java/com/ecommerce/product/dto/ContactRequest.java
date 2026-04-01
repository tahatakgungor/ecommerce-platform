package com.ecommerce.product.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequest {

    @NotBlank(message = "Ad soyad boş olamaz")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "E-posta boş olamaz")
    @Email(message = "Geçerli bir e-posta adresi girin")
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String company;

    @NotBlank(message = "Mesaj boş olamaz")
    @Size(max = 2000)
    private String message;
}
