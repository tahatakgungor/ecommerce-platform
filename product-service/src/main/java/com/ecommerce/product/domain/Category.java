package com.ecommerce.product.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @JsonProperty("_id")
    public String getHarriId() {
        return id != null ? id.toString() : null;
    }

    // Frontend {item.parent} okuduğu için ismi buraya set etmeliyiz
    @JsonProperty("parent")
    private String parentName;

    // Frontend {item.children.map((sub: string...))} beklediği için
    // burası Nesne listesi değil, String listesi olmalı!
    @Transient
    @JsonProperty("children")
    private List<String> children = new java.util.ArrayList<>();
}