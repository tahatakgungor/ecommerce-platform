package com.ecommerce.product.application;

import com.ecommerce.product.domain.*;
import com.ecommerce.product.dto.review.*;
import com.ecommerce.product.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository productReviewRepository;
    private final ProductReviewFeedbackRepository feedbackRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final FileUploadService fileUploadService;
    private final ObjectMapper objectMapper;

    @Transactional
    @CacheEvict(cacheNames = "productReviewSummary", key = "#productId")
    public ReviewResponse createReview(UUID productId, ReviewCreateRequest request, String userEmail) {
        validateReviewRequest(request);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı."));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        ReviewEligibilityResponse eligibility = evaluateEligibility(productId, user);
        if (!eligibility.isCanReview()) {
            throw new RuntimeException(eligibility.getReason());
        }

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setCommentTitle(trimNullable(request.getCommentTitle()));
        review.setCommentBody(trimNullable(request.getCommentBody()));
        review.setStatus(resolveInitialStatus(review));
        review.setVerifiedPurchase(true);
        review.setMediaUrls(writeMediaUrls(sanitizeMediaUrls(request.getMediaUrls())));

        ProductReview saved = productReviewRepository.save(review);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ReviewEligibilityResponse getReviewEligibility(UUID productId, String userEmail) {
        ensureProductExists(productId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        return evaluateEligibility(productId, user);
    }

    @Transactional
    public String uploadReviewMedia(UUID productId, org.springframework.web.multipart.MultipartFile file, String userEmail) {
        ensureProductExists(productId);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        if (!isCustomerRole(user.getRole())) {
            throw new RuntimeException("Sadece müşteri hesapları görsel yükleyebilir.");
        }
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Yüklenecek görsel bulunamadı.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Görsel boyutu 5MB'dan büyük olamaz.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new RuntimeException("Sadece görsel dosyaları yüklenebilir.");
        }

        return fileUploadService.saveFile(file);
    }

    @Transactional
    @CacheEvict(cacheNames = "productReviewSummary", key = "#productId")
    public ReviewResponse updateOwnReview(UUID productId, UUID reviewId, ReviewUpdateRequest request, String userEmail) {
        validateReviewRequest(request);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        ProductReview review = productReviewRepository.findByIdAndProductId(reviewId, productId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı."));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Sadece kendi yorumunuzu düzenleyebilirsiniz.");
        }

        review.setRating(request.getRating());
        review.setCommentTitle(trimNullable(request.getCommentTitle()));
        review.setCommentBody(trimNullable(request.getCommentBody()));
        review.setMediaUrls(writeMediaUrls(sanitizeMediaUrls(request.getMediaUrls())));
        review.setStatus(resolveInitialStatus(review)); // düzenleme sonrası tekrar moderasyon kuyruğu

        return toResponse(productReviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getApprovedReviews(UUID productId,
                                                  String sort,
                                                  boolean withMedia,
                                                  int page,
                                                  int size) {
        ensureProductExists(productId);

        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), resolveSort(sort));
        Page<ProductReview> reviews = withMedia
                ? productReviewRepository.findByProductIdAndStatusWithMedia(productId, ReviewStatus.APPROVED, pageable)
                : productReviewRepository.findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable);

        List<ReviewResponse> items = reviews.getContent().stream().map(this::toResponse).toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reviews", items);
        result.put("summary", getReviewSummary(productId));
        result.put("page", reviews.getNumber());
        result.put("size", reviews.getSize());
        result.put("totalPages", reviews.getTotalPages());
        result.put("totalElements", reviews.getTotalElements());
        result.put("hasNext", reviews.hasNext());
        result.put("hasPrevious", reviews.hasPrevious());
        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productReviewSummary", key = "#productId")
    public ReviewSummaryResponse getReviewSummary(UUID productId) {
        ensureProductExists(productId);

        List<Object[]> avgRows = productReviewRepository.findAverageAndCountByProductAndStatus(productId, ReviewStatus.APPROVED);
        Object avgRaw = null;
        Object totalRaw = null;
        if (avgRows != null && !avgRows.isEmpty()) {
            Object[] row = avgRows.get(0);
            if (row != null && row.length == 1 && row[0] instanceof Object[] nested) {
                row = nested;
            }
            if (row != null && row.length > 0) avgRaw = row[0];
            if (row != null && row.length > 1) totalRaw = row[1];
        }
        double avg = toDouble(avgRaw, 0.0);
        long total = toLong(totalRaw, 0L);

        Map<Integer, Long> starCounts = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) starCounts.put(i, 0L);

        for (Object[] row : productReviewRepository.findRatingDistributionByProductAndStatus(productId, ReviewStatus.APPROVED)) {
            int star = toInt(row != null && row.length > 0 ? row[0] : null, -1);
            long count = toLong(row != null && row.length > 1 ? row[1] : null, 0L);
            if (star >= 1 && star <= 5) starCounts.put(star, count);
        }

        Map<Integer, Double> starPercentages = new LinkedHashMap<>();
        for (Map.Entry<Integer, Long> e : starCounts.entrySet()) {
            double percentage = total == 0 ? 0.0 : (e.getValue() * 100.0) / total;
            starPercentages.put(e.getKey(), round2(percentage));
        }

        return ReviewSummaryResponse.builder()
                .averageRating(round2(avg))
                .totalReviews(total)
                .starCounts(starCounts)
                .starPercentages(starPercentages)
                .build();
    }

    @Transactional
    public Map<String, Object> voteReview(UUID productId, UUID reviewId, boolean helpful, String userEmail) {
        User voter = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı."));

        ProductReview review = productReviewRepository.findByIdAndProductId(reviewId, productId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı."));

        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new RuntimeException("Sadece onaylanmış yorumlar oylanabilir.");
        }

        if (review.getUser().getId().equals(voter.getId())) {
            throw new RuntimeException("Kendi yorumunuzu oylayamazsınız.");
        }

        ReviewVoteType newType = helpful ? ReviewVoteType.HELPFUL : ReviewVoteType.NOT_HELPFUL;
        Optional<ProductReviewFeedback> existingOpt = feedbackRepository.findByReviewIdAndUserId(review.getId(), voter.getId());

        if (existingOpt.isEmpty()) {
            ProductReviewFeedback feedback = new ProductReviewFeedback();
            feedback.setReview(review);
            feedback.setUser(voter);
            feedback.setVoteType(newType);
            feedbackRepository.save(feedback);
            applyVoteDelta(review, newType, +1);
        } else {
            ProductReviewFeedback existing = existingOpt.get();
            if (existing.getVoteType() != newType) {
                applyVoteDelta(review, existing.getVoteType(), -1);
                existing.setVoteType(newType);
                feedbackRepository.save(existing);
                applyVoteDelta(review, newType, +1);
            }
        }

        productReviewRepository.save(review);

        return Map.of(
                "reviewId", review.getId(),
                "helpfulCount", review.getHelpfulCount(),
                "notHelpfulCount", review.getNotHelpfulCount()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getModerationReviews(String status, int page, int size) {
        ReviewStatus reviewStatus = parseStatus(status, ReviewStatus.PENDING);
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ProductReview> reviews = productReviewRepository.findByStatus(reviewStatus, pageable);

        return Map.of(
                "reviews", reviews.getContent().stream().map(this::toResponse).toList(),
                "status", reviewStatus.name(),
                "page", reviews.getNumber(),
                "size", reviews.getSize(),
                "totalPages", reviews.getTotalPages(),
                "totalElements", reviews.getTotalElements()
        );
    }

    @Transactional
    @CacheEvict(cacheNames = "productReviewSummary", key = "#result.productId")
    public ReviewResponse updateReviewStatus(UUID reviewId, String status) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Yorum bulunamadı."));

        review.setStatus(parseStatus(status, review.getStatus()));
        return toResponse(productReviewRepository.save(review));
    }

    private void applyVoteDelta(ProductReview review, ReviewVoteType type, int delta) {
        if (type == ReviewVoteType.HELPFUL) {
            long updated = Math.max(0L, (review.getHelpfulCount() == null ? 0L : review.getHelpfulCount()) + delta);
            review.setHelpfulCount(updated);
        } else {
            long updated = Math.max(0L, (review.getNotHelpfulCount() == null ? 0L : review.getNotHelpfulCount()) + delta);
            review.setNotHelpfulCount(updated);
        }
    }

    private ReviewResponse toResponse(ProductReview r) {
        return ReviewResponse.builder()
                .reviewId(r.getId())
                .productId(r.getProduct().getId())
                .userId(r.getUser().getId())
                .userName(maskDisplayName(r.getUser().getName()))
                .rating(r.getRating())
                .commentTitle(r.getCommentTitle())
                .commentBody(r.getCommentBody())
                .status(r.getStatus().name())
                .verifiedPurchase(r.isVerifiedPurchase())
                .mediaUrls(readMediaUrls(r.getMediaUrls()))
                .helpfulCount(r.getHelpfulCount() == null ? 0L : r.getHelpfulCount())
                .notHelpfulCount(r.getNotHelpfulCount() == null ? 0L : r.getNotHelpfulCount())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private String maskDisplayName(String name) {
        String value = trimNullable(name);
        if (value == null) return "Kullanıcı";

        String[] parts = value.split("\\s+");
        List<String> masked = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) continue;
            String first = part.substring(0, 1).toUpperCase(Locale.ROOT);
            masked.add(first + "***");
        }
        return masked.isEmpty() ? "Kullanıcı" : String.join(" ", masked);
    }

    private boolean hasDeliveredPurchase(UUID userId, UUID productId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId.toString())
                .stream()
                .filter(order -> "delivered".equalsIgnoreCase(trimNullable(order.getStatus())))
                .map(Order::getCart)
                .filter(Objects::nonNull)
                .flatMap(cart -> parseCartItems(cart).stream())
                .map(this::extractProductIdFromCartItem)
                .anyMatch(productId::equals);
    }

    private ReviewEligibilityResponse evaluateEligibility(UUID productId, User user) {
        if (!isCustomerRole(user.getRole())) {
            return ReviewEligibilityResponse.builder()
                    .canReview(false)
                    .deliveredPurchase(false)
                    .alreadyReviewed(false)
                    .reason("Sadece müşteri hesapları ürün değerlendirmesi yapabilir.")
                    .build();
        }

        boolean deliveredPurchase = hasDeliveredPurchase(user.getId(), productId);
        boolean alreadyReviewed = productReviewRepository.findByProductIdAndUserId(productId, user.getId()).isPresent();

        String reason = null;
        if (!deliveredPurchase) {
            reason = "Yorum yapabilmek için ürün siparişinizin teslim edilmiş olması gerekir.";
        } else if (alreadyReviewed) {
            reason = "Bu ürün için zaten bir yorumunuz var. Düzenleme yapabilirsiniz.";
        }

        return ReviewEligibilityResponse.builder()
                .canReview(deliveredPurchase && !alreadyReviewed)
                .deliveredPurchase(deliveredPurchase)
                .alreadyReviewed(alreadyReviewed)
                .reason(reason)
                .build();
    }

    private boolean isCustomerRole(String role) {
        String normalized = trimNullable(role);
        if (normalized == null) return false;
        String upper = normalized.toUpperCase(Locale.ROOT);
        return "CUSTOMER".equals(upper) || "ROLE_CUSTOMER".equals(upper) || "USER".equals(upper) || "ROLE_USER".equals(upper);
    }

    private List<Map<String, Object>> parseCartItems(String cartJson) {
        if (cartJson == null || cartJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(cartJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private UUID extractProductIdFromCartItem(Map<String, Object> item) {
        if (item == null) return null;
        Object idValue = item.get("_id") != null ? item.get("_id") : item.get("id");
        if (idValue == null) return null;
        try {
            return UUID.fromString(idValue.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> sanitizeMediaUrls(List<String> mediaUrls) {
        if (mediaUrls == null || mediaUrls.isEmpty()) return List.of();

        List<String> normalized = mediaUrls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .limit(5)
                .toList();

        for (String media : normalized) {
            String lower = media.toLowerCase(Locale.ROOT);
            if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
                throw new RuntimeException("Medya bağlantısı http/https ile başlamalıdır.");
            }
            if (media.length() > 500) {
                throw new RuntimeException("Medya bağlantısı çok uzun.");
            }
        }

        return normalized;
    }

    private String writeMediaUrls(List<String> mediaUrls) {
        try {
            return objectMapper.writeValueAsString(mediaUrls == null ? List.of() : mediaUrls);
        } catch (Exception ex) {
            throw new RuntimeException("Medya verisi işlenemedi.");
        }
    }

    private List<String> readMediaUrls(String mediaUrlsJson) {
        if (mediaUrlsJson == null || mediaUrlsJson.isBlank()) return List.of();
        try {
            return objectMapper.readValue(mediaUrlsJson, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private ReviewStatus resolveInitialStatus(ProductReview review) {
        String text = ((review.getCommentTitle() == null ? "" : review.getCommentTitle()) + " " +
                (review.getCommentBody() == null ? "" : review.getCommentBody())).toLowerCase(Locale.ROOT);

        List<String> blockedWords = List.of("bahis", "casino", "xxx", "kumar", "bedava para", "yatırım tavsiyesi");
        boolean containsBlockedWord = blockedWords.stream().anyMatch(text::contains);
        return containsBlockedWord ? ReviewStatus.REJECTED : ReviewStatus.APPROVED;
    }

    private void validateReviewRequest(ReviewCreateRequest request) {
        if (request == null) throw new RuntimeException("Yorum verisi boş olamaz.");
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Puan 1 ile 5 arasında olmalıdır.");
        }
        String commentBody = trimNullable(request.getCommentBody());
        if (commentBody == null || commentBody.length() < 5) {
            throw new RuntimeException("Yorum metni en az 5 karakter olmalıdır.");
        }
        if (commentBody.length() > 2000) {
            throw new RuntimeException("Yorum metni en fazla 2000 karakter olabilir.");
        }
        String title = trimNullable(request.getCommentTitle());
        if (title != null && title.length() > 120) {
            throw new RuntimeException("Yorum başlığı en fazla 120 karakter olabilir.");
        }
    }

    private void validateReviewRequest(ReviewUpdateRequest request) {
        if (request == null) throw new RuntimeException("Yorum verisi boş olamaz.");
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new RuntimeException("Puan 1 ile 5 arasında olmalıdır.");
        }
        String commentBody = trimNullable(request.getCommentBody());
        if (commentBody == null || commentBody.length() < 5) {
            throw new RuntimeException("Yorum metni en az 5 karakter olmalıdır.");
        }
        if (commentBody.length() > 2000) {
            throw new RuntimeException("Yorum metni en fazla 2000 karakter olabilir.");
        }
        String title = trimNullable(request.getCommentTitle());
        if (title != null && title.length() > 120) {
            throw new RuntimeException("Yorum başlığı en fazla 120 karakter olabilir.");
        }
    }

    private Sort resolveSort(String sort) {
        String value = sort == null ? "newest" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "highest", "highest_rating" -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
            case "lowest", "lowest_rating" -> Sort.by(Sort.Order.asc("rating"), Sort.Order.desc("createdAt"));
            case "most_helpful" -> Sort.by(Sort.Order.desc("helpfulCount"), Sort.Order.desc("createdAt"));
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), 50);
    }

    private void ensureProductExists(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Ürün bulunamadı.");
        }
    }

    private ReviewStatus parseStatus(String status, ReviewStatus defaultStatus) {
        if (status == null || status.isBlank()) return defaultStatus;
        try {
            return ReviewStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new RuntimeException("Geçersiz durum. Kullanılabilir: PENDING, APPROVED, REJECTED");
        }
    }

    private String trimNullable(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private long toLong(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof Object[] arr && arr.length > 0) return toLong(arr[0], defaultValue);
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof Object[] arr && arr.length > 0) return toInt(arr[0], defaultValue);
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private double toDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof Object[] arr && arr.length > 0) return toDouble(arr[0], defaultValue);
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception ex) {
            return defaultValue;
        }
    }
}
