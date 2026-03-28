package com.ecommerce.product.application.event;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ProductCreatedEvent {
    private UUID productId;
    private String name;        // Eventlerde genellikle 'name' kalması daha iyidir (İş mantığı)
    private String sku;         // YENİ: Diğer servisler SKU üzerinden eşleme yapabilir
    private Double price;       // Entity ile uyumlu olması için Double (veya BigDecimal kalsın dersen cast etmelisin)
    private Integer stock;      // YENİ: Stok servisi bu bilgiyi bekler
    private String status;      // YENİ: Ürün aktif mi pasif mi?
    private LocalDateTime createdAt;
}