package com.ecommerce.product.dto.returns;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReturnRequest {

    @NotBlank(message = "İade nedeni zorunludur.")
    @Size(max = 500)
    private String reason;

    @Size(max = 2000)
    private String customerNote;
}
