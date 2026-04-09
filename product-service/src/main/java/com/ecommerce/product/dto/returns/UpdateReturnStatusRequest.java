package com.ecommerce.product.dto.returns;

import com.ecommerce.product.domain.OrderReturnStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReturnStatusRequest {

    @NotNull(message = "Durum zorunludur.")
    private OrderReturnStatus status;

    @Size(max = 2000)
    private String adminNote;
}
