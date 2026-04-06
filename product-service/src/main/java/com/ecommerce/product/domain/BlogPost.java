package com.ecommerce.product.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blog_posts")
@Data
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, length = 220)
    private String title;

    @Column(nullable = false, unique = true, length = 240)
    private String slug;

    @Column(length = 1200)
    private String summary;

    @Column(length = 1000)
    private String coverImage;

    @Lob
    private String contentHtml;

    @ElementCollection
    @CollectionTable(name = "blog_post_related_products", joinColumns = @JoinColumn(name = "blog_post_id"))
    @Column(name = "product_id")
    private List<String> relatedProductIds = new ArrayList<>();

    @Column(nullable = false, length = 24)
    private String status = "draft";

    private LocalDateTime publishedAt;

    @Column(length = 220)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null || this.status.isBlank()) {
            this.status = "draft";
        }
        if (this.relatedProductIds == null) {
            this.relatedProductIds = new ArrayList<>();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now(ZoneId.of("Europe/Istanbul"));
        if (this.status == null || this.status.isBlank()) {
            this.status = "draft";
        }
        if (this.relatedProductIds == null) {
            this.relatedProductIds = new ArrayList<>();
        }
    }
}
