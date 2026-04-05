package com.ecommerce.product.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(name = "brands")
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String email;
    private String website;
    private String location;
    private String description;
    @JsonAlias({"image", "img"})
    private String logo;

    // Frontend uyumluluğu için
    @JsonProperty("_id")
    public String getHarriId() {
        return id != null ? id.toString() : null;
    }

    // Varsayılan durum
    private String status = "Active";
}
