package com.ecommerce.product.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Frontend "parent" olarak gönderse de "name" olarak gönderse de bu alana dolsun
    @Column(nullable = false, unique = true)
    @JsonAlias({"parent", "categoryName"})
    private String name;

    private String description;
    private String image;

    @JsonProperty("_id")
    public String getHarriId() {
        return id != null ? id.toString() : null;
    }

    // Listeleme yapılırken Frontend "parent" anahtarını beklediği için:
    @JsonProperty("parent")
    public String getParentValue() {
        return name;
    }

    @Transient
    @JsonProperty("children")
    private List<String> children = new ArrayList<>();
}