package com.ecommerce.product.application;

import com.ecommerce.product.domain.BlogPost;
import com.ecommerce.product.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogPostService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_PUBLISHED = "published";
    private static final ZoneId ISTANBUL_ZONE = ZoneId.of("Europe/Istanbul");

    private final BlogPostRepository blogPostRepository;

    @Transactional(readOnly = true)
    public List<BlogPost> getPublishedPosts() {
        return blogPostRepository.findByStatusOrderByPublishedAtDescUpdatedAtDesc(STATUS_PUBLISHED);
    }

    @Transactional(readOnly = true)
    public BlogPost getPublishedPostBySlug(String slug) {
        String normalizedSlug = sanitizeSlug(slug);
        return blogPostRepository.findBySlugAndStatus(normalizedSlug, STATUS_PUBLISHED)
                .orElseThrow(() -> new RuntimeException("Blog yazısı bulunamadı."));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public List<BlogPost> getAllForAdmin() {
        return blogPostRepository.findAllByOrderByUpdatedAtDesc();
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public BlogPost create(BlogPost request) {
        validateRequest(request, null);
        BlogPost post = new BlogPost();
        applyFields(post, request, null);
        return blogPostRepository.save(post);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public BlogPost update(UUID id, BlogPost request) {
        BlogPost existing = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog yazısı bulunamadı."));
        validateRequest(request, id);
        applyFields(existing, request, existing);
        return blogPostRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public BlogPost updateStatus(UUID id, String status) {
        BlogPost existing = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog yazısı bulunamadı."));
        String normalized = normalizeStatus(status);
        existing.setStatus(normalized);
        if (STATUS_PUBLISHED.equals(normalized)) {
            if (existing.getPublishedAt() == null) {
                existing.setPublishedAt(LocalDateTime.now(ISTANBUL_ZONE));
            }
        } else {
            existing.setPublishedAt(null);
        }
        return blogPostRepository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('Admin','Staff')")
    public void delete(UUID id) {
        BlogPost existing = blogPostRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blog yazısı bulunamadı."));
        blogPostRepository.delete(existing);
    }

    private void validateRequest(BlogPost request, UUID currentId) {
        if (request == null) {
            throw new RuntimeException("Geçersiz blog verisi.");
        }

        String title = trimOrEmpty(request.getTitle());
        String contentHtml = sanitizeHtmlContent(request.getContentHtml());
        if (title.isBlank()) {
            throw new RuntimeException("Blog başlığı zorunludur.");
        }
        if (contentHtml.isBlank()) {
            throw new RuntimeException("Blog içeriği zorunludur.");
        }

        String resolvedSlug = resolveSlug(request);
        boolean slugExists = currentId == null
                ? blogPostRepository.existsBySlug(resolvedSlug)
                : blogPostRepository.existsBySlugAndIdNot(resolvedSlug, currentId);
        if (slugExists) {
            throw new RuntimeException("Bu blog link adı zaten kullanılıyor.");
        }
    }

    private void applyFields(BlogPost target, BlogPost source, BlogPost existing) {
        target.setTitle(stripHtmlTags(trimOrEmpty(source.getTitle())));
        target.setSlug(resolveSlug(source));
        target.setSummary(stripAndNull(source.getSummary()));
        target.setCoverImage(sanitizeCoverImage(source.getCoverImage()));
        target.setContentHtml(sanitizeHtmlContent(source.getContentHtml()));
        target.setSeoTitle(stripAndNull(source.getSeoTitle()));
        target.setSeoDescription(stripAndNull(source.getSeoDescription()));
        target.setRelatedProductIds(sanitizeRelatedIds(source.getRelatedProductIds()));

        String normalizedStatus = normalizeStatus(source.getStatus());
        target.setStatus(normalizedStatus);

        if (STATUS_PUBLISHED.equals(normalizedStatus)) {
            LocalDateTime publishedAt = source.getPublishedAt();
            if (publishedAt == null && existing != null) {
                publishedAt = existing.getPublishedAt();
            }
            target.setPublishedAt(publishedAt != null ? publishedAt : LocalDateTime.now(ISTANBUL_ZONE));
        } else {
            target.setPublishedAt(null);
        }
    }

    private List<String> sanitizeRelatedIds(List<String> relatedProductIds) {
        if (relatedProductIds == null || relatedProductIds.isEmpty()) {
            return new ArrayList<>();
        }
        return relatedProductIds.stream()
                .map(this::trimOrEmpty)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private String resolveSlug(BlogPost source) {
        String candidate = trimOrEmpty(source.getSlug());
        if (candidate.isBlank()) {
            candidate = trimOrEmpty(source.getTitle());
        }
        String sanitized = sanitizeSlug(candidate);
        if (sanitized.isBlank()) {
            throw new RuntimeException("Geçerli bir blog link adı üretilemedi.");
        }
        if (sanitized.length() > 240) {
            return sanitized.substring(0, 240);
        }
        return sanitized;
    }

    private String normalizeStatus(String status) {
        String normalized = trimOrEmpty(status).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return STATUS_DRAFT;
        }
        if (!STATUS_DRAFT.equals(normalized) && !STATUS_PUBLISHED.equals(normalized)) {
            throw new RuntimeException("Durum geçersiz. Kullanılabilir: draft, published");
        }
        return normalized;
    }

    private String sanitizeSlug(String value) {
        String normalized = trimOrEmpty(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }

        Map<String, String> trMap = Map.of(
                "ç", "c",
                "ğ", "g",
                "ı", "i",
                "ö", "o",
                "ş", "s",
                "ü", "u"
        );

        for (Map.Entry<String, String> entry : trMap.entrySet()) {
            normalized = normalized.replace(entry.getKey(), entry.getValue());
        }

        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");

        return normalized;
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimOrNull(String value) {
        String trimmed = trimOrEmpty(value);
        return trimmed.isBlank() ? null : trimmed;
    }

    private String stripAndNull(String value) {
        String stripped = stripHtmlTags(trimOrEmpty(value));
        return stripped.isBlank() ? null : stripped;
    }

    private String stripHtmlTags(String value) {
        return trimOrEmpty(value)
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sanitizeHtmlContent(String value) {
        String html = trimOrEmpty(value);
        if (html.isBlank()) {
            return html;
        }
        return html
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("(?is)<style[^>]*>.*?</style>", "")
                .replaceAll("(?is)<iframe[^>]*>.*?</iframe>", "")
                .replaceAll("(?i)\\son\\w+\\s*=\\s*\"[^\"]*\"", "")
                .replaceAll("(?i)\\son\\w+\\s*=\\s*'[^']*'", "")
                .replaceAll("(?i)\\shref\\s*=\\s*\"javascript:[^\"]*\"", " href=\"#\"")
                .replaceAll("(?i)\\ssrc\\s*=\\s*\"javascript:[^\"]*\"", "")
                .trim();
    }

    private String sanitizeCoverImage(String value) {
        String cover = trimOrNull(value);
        if (cover == null) {
            return null;
        }
        String lowered = cover.toLowerCase(Locale.ROOT);
        if (lowered.startsWith("javascript:") || lowered.startsWith("data:text/html")) {
            return null;
        }
        if (cover.startsWith("/") || lowered.startsWith("http://") || lowered.startsWith("https://")) {
            return cover;
        }
        return null;
    }
}
