package com.ecommerce.product.config;

import com.ecommerce.product.domain.Product;
import com.ecommerce.product.domain.ProductReview;
import com.ecommerce.product.domain.ReviewStatus;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.ProductReviewRepository;
import com.ecommerce.product.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@RequiredArgsConstructor
@Order(4)
@Slf4j
public class ProductReviewRatingSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Value("${app.seed-product-review-ratings:false}")
    private boolean seedProductReviewRatings;

    @Value("${app.seed-product-review-ratings-reset-existing:false}")
    private boolean resetExisting;

    @Value("${app.seed-product-review-ratings-min-per-product:18}")
    private int minPerProduct;

    @Value("${app.seed-product-review-ratings-max-per-product:36}")
    private int maxPerProduct;

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedProductReviewRatings) return;

        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            log.info("Review seed atlandı: ürün bulunamadı.");
            return;
        }

        if (maxPerProduct < minPerProduct) {
            maxPerProduct = minPerProduct;
        }

        List<User> customers = ensureCustomerPool(Math.max(maxPerProduct + 12, 48));
        int seededCount = 0;

        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            List<ProductReview> existing = productReviewRepository.findByProductId(product.getId());
            if (resetExisting && !existing.isEmpty()) {
                productReviewRepository.deleteAll(existing);
                existing = List.of();
            }

            Set<UUID> usedUserIds = new HashSet<>();
            for (ProductReview r : existing) {
                if (r.getUser() != null && r.getUser().getId() != null) {
                    usedUserIds.add(r.getUser().getId());
                }
            }

            int targetCount = randomBetween(minPerProduct, maxPerProduct);
            int currentCount = existing.size();
            int need = Math.max(0, targetCount - currentCount);
            if (need == 0) continue;

            List<User> available = new ArrayList<>(customers.stream()
                    .filter(u -> u.getId() != null && !usedUserIds.contains(u.getId()))
                    .toList());

            if (available.size() < need) {
                customers = ensureCustomerPool(customers.size() + (need - available.size()) + 20);
                available = new ArrayList<>(customers.stream()
                        .filter(u -> u.getId() != null && !usedUserIds.contains(u.getId()))
                        .toList());
            }

            Collections.shuffle(available, random);
            List<ProductReview> toSave = new ArrayList<>();

            for (int j = 0; j < need && j < available.size(); j++) {
                User user = available.get(j);
                ProductReview review = new ProductReview();
                review.setProduct(product);
                review.setUser(user);
                review.setRating(pickRating(i));
                review.setCommentTitle(pickTitle(review.getRating()));
                review.setCommentBody(pickComment(product, review.getRating()));
                review.setStatus(ReviewStatus.APPROVED);
                review.setVerifiedPurchase(true);
                review.setMediaUrls(pickMediaUrls(review.getRating()));
                review.setHelpfulCount((long) randomBetween(0, 34));
                review.setNotHelpfulCount((long) randomBetween(0, 4));
                toSave.add(review);
            }

            productReviewRepository.saveAll(toSave);
            seededCount += toSave.size();
        }

        verifyAverages(products);
        log.info("Review seed tamamlandı. Eklenen/güncellenen review adedi: {}", seededCount);
    }

    private List<User> ensureCustomerPool(int targetSize) {
        Map<UUID, User> uniqueCustomers = new LinkedHashMap<>();
        for (User user : userRepository.findByRole("Customer")) {
            if (user.getId() != null) {
                uniqueCustomers.put(user.getId(), user);
            }
        }
        List<User> customers = new ArrayList<>(uniqueCustomers.values());
        int index = 1;
        while (customers.size() < targetSize) {
            String email = "seed.customer." + String.format("%03d", index) + "@serravit.local";
            Optional<User> existing = userRepository.findByEmail(email);
            if (existing.isPresent()) {
                User existingUser = existing.get();
                if ("Customer".equalsIgnoreCase(existingUser.getRole())
                        && existingUser.getId() != null
                        && !uniqueCustomers.containsKey(existingUser.getId())) {
                    uniqueCustomers.put(existingUser.getId(), existingUser);
                    customers.add(existingUser);
                }
                index++;
                continue;
            }

            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode("SeedCustomer!123"));
            user.setName("Müşteri " + index);
            user.setRole("Customer");
            user.setEmailVerified(true);
            user.setCity("Kocaeli");
            user.setCountry("Türkiye");
            user = userRepository.save(user);
            if (user.getId() != null && !uniqueCustomers.containsKey(user.getId())) {
                uniqueCustomers.put(user.getId(), user);
                customers.add(user);
            }
            index++;
        }
        return new ArrayList<>(uniqueCustomers.values());
    }

    private int pickRating(int productIndex) {
        double fiveThreshold = 0.68 + ((productIndex % 5) * 0.04); // 0.68 .. 0.84
        double fourThreshold = 0.98; // kalan %2 -> 3 yıldız
        double roll = random.nextDouble();
        if (roll < fiveThreshold) return 5;
        if (roll < fourThreshold) return 4;
        return 3;
    }

    private String pickTitle(int rating) {
        List<String> titles5 = List.of("Kesinlikle tavsiye ederim", "Beklentimin üzerinde", "Çok memnun kaldım", "Kaliteli ürün");
        List<String> titles4 = List.of("Genel olarak çok iyi", "Fiyat performans başarılı", "Memnun edici", "Tekrar alırım");
        List<String> titles3 = List.of("Orta seviye ama iş görüyor", "Fena değil", "Beklentiyi büyük ölçüde karşılıyor");
        if (rating >= 5) return titles5.get(random.nextInt(titles5.size()));
        if (rating == 4) return titles4.get(random.nextInt(titles4.size()));
        return titles3.get(random.nextInt(titles3.size()));
    }

    private String pickComment(Product product, int rating) {
        String productName = product.getName() == null ? "Ürün" : product.getName();
        List<String> comments5 = List.of(
                productName + " düzenli kullanımda etkisini net gösterdi, paketleme de çok iyiydi.",
                "Sipariş hızlı geldi, ürün açıklamasıyla birebir uyumlu çıktı.",
                "Kullanım kolay, kalite hissi yüksek. Aynı ürünü tekrar sipariş edeceğim."
        );
        List<String> comments4 = List.of(
                "Genel olarak çok memnunum, sadece kargo bir gün gecikti.",
                "Ürün kaliteli ve beklentiyi karşılıyor, fiyatı da dengeli.",
                "İçerik ve etki başarılı, tekrar almayı düşünüyorum."
        );
        List<String> comments3 = List.of(
                "Ürün kötü değil, etkisi kişiden kişiye değişebilir.",
                "Genel olarak kullanılabilir seviyede, teslimat sorunsuzdu."
        );
        if (rating >= 5) return comments5.get(random.nextInt(comments5.size()));
        if (rating == 4) return comments4.get(random.nextInt(comments4.size()));
        return comments3.get(random.nextInt(comments3.size()));
    }

    private String pickMediaUrls(int rating) {
        if (rating < 4 || random.nextDouble() > 0.18) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(List.of(
                    "https://placehold.co/800x800/f5f5f5/333333?text=Review+Photo"
            ));
        } catch (Exception ex) {
            return "[]";
        }
    }

    private int randomBetween(int min, int max) {
        if (max <= min) return min;
        return min + random.nextInt((max - min) + 1);
    }

    private void verifyAverages(List<Product> products) {
        for (Product product : products) {
            List<Object[]> avgRows = productReviewRepository.findAverageAndCountByProductAndStatus(
                    product.getId(), ReviewStatus.APPROVED
            );
            if (avgRows == null || avgRows.isEmpty() || avgRows.get(0) == null || avgRows.get(0).length < 2) {
                continue;
            }
            Object[] row = avgRows.get(0);
            double avg = row[0] instanceof Number n ? n.doubleValue() : 0.0;
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            if (count > 0 && avg < 4.5) {
                log.warn("Review seed sonrası ortalama 4.5 altında kaldı -> productId={}, avg={}, count={}",
                        product.getId(), avg, count);
            }
        }
    }

}
