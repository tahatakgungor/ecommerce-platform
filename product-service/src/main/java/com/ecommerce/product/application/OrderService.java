package com.ecommerce.product.application;

import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.domain.Coupon;
import com.ecommerce.product.repository.CouponRepository;
import com.ecommerce.product.repository.OrderRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.stripe.secret-key:}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey;
        }
    }

    // ---------- Stripe: Payment Intent ----------

    public Map<String, Object> createPaymentIntent(Map<String, Object> body, String email) {
        CheckoutSummary checkoutSummary = calculateCheckout(body, email);
        long amountInKurus = Math.round(checkoutSummary.totalAmount() * 100.0);
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInKurus)
                    .setCurrency("try")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            return Map.of(
                    "clientSecret", intent.getClientSecret(),
                    "subTotal", formatAmount(checkoutSummary.subTotal()),
                    "shippingCost", formatAmount(checkoutSummary.shippingCost()),
                    "discount", formatAmount(checkoutSummary.discountAmount()),
                    "totalAmount", formatAmount(checkoutSummary.totalAmount())
            );
        } catch (Exception e) {
            log.error("Stripe PaymentIntent oluşturulamadı: {}", e.getMessage());
            throw new RuntimeException("Ödeme başlatılamadı: " + e.getMessage());
        }
    }

    // ---------- Sipariş kaydet ----------

    @Transactional
    public Map<String, Object> addOrder(Map<String, Object> body, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));
        CheckoutSummary checkoutSummary = calculateCheckout(body, email);
        Order order = new Order();

        order.setName(str(body, "name"));
        order.setAddress(str(body, "address"));
        order.setContact(str(body, "contact"));
        order.setEmail(str(body, "email"));
        order.setCity(str(body, "city"));
        order.setCountry(str(body, "country"));
        order.setZipCode(str(body, "zipCode"));
        order.setShippingOption(str(body, "shippingOption"));
        order.setStatus("pending");
        order.setUserId(user.getId().toString());

        order.setSubTotal(checkoutSummary.subTotal());
        order.setShippingCost(checkoutSummary.shippingCost());
        order.setDiscount(checkoutSummary.discountAmount());
        order.setTotalAmount(checkoutSummary.totalAmount());
        order.setCouponCode(checkoutSummary.couponCode());
        order.setCouponTitle(checkoutSummary.couponTitle());

        // Cart, cardInfo, paymentIntent → JSON string olarak sakla
        order.setCart(toJson(checkoutSummary.sanitizedCart()));
        order.setCardInfo(toJson(body.get("cardInfo")));
        order.setPaymentIntent(toJson(body.get("paymentIntent")));

        Order saved = orderRepository.save(order);
        return Map.of("success", true, "order", toResponse(saved));
    }

    // ---------- Kullanıcının siparişleri ----------

    public Map<String, Object> getOrdersByUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        List<Map<String, Object>> orders = orderRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId().toString())
                .stream()
                .map(this::toResponse)
                .toList();

        return Map.of("orders", orders);
    }

    // ---------- Tek sipariş ----------

    public Map<String, Object> getSingleOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı!"));
        return Map.of("order", toResponse(order));
    }

    public Map<String, Object> getSingleOrderForUser(String email, UUID id) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı!"));

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı!"));

        if (!user.getId().toString().equals(order.getUserId())) {
            throw new RuntimeException("Bu siparişi görüntüleme yetkiniz yok!");
        }

        return Map.of("order", toResponse(order));
    }

    // ---------- Admin: tüm siparişler ----------

    public Map<String, Object> getAllOrders() {
        List<Map<String, Object>> orders = orderRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();
        return Map.of("orders", orders, "total", (long) orders.size());
    }

    // ---------- Admin: durum güncelle ----------

    @Transactional
    public Map<String, Object> updateOrderStatus(UUID id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı!"));
        order.setStatus(status);
        return Map.of("order", toResponse(orderRepository.save(order)));
    }

    // ---------- Yardımcı metotlar ----------

    private Map<String, Object> toResponse(Order o) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_id", o.getId().toString());
        map.put("name", o.getName());
        map.put("address", o.getAddress());
        map.put("contact", o.getContact());
        map.put("email", o.getEmail());
        map.put("city", o.getCity());
        map.put("country", o.getCountry());
        map.put("zipCode", o.getZipCode());
        map.put("shippingOption", o.getShippingOption());
        map.put("status", o.getStatus());
        map.put("userId", o.getUserId());
        map.put("invoice", o.getInvoice());
        map.put("subTotal", o.getSubTotal());
        map.put("shippingCost", o.getShippingCost());
        map.put("discount", o.getDiscount());
        map.put("totalAmount", o.getTotalAmount());
        map.put("couponCode", o.getCouponCode());
        map.put("couponTitle", o.getCouponTitle());
        map.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        map.put("cart", fromJson(o.getCart()));
        map.put("cardInfo", fromJson(o.getCardInfo()));
        map.put("paymentIntent", fromJson(o.getPaymentIntent()));
        return map;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private Object fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Object>() {});
        } catch (Exception e) {
            return json;
        }
    }

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Double toDouble(Object val) {
        if (val == null) return 0.0;
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private CheckoutSummary calculateCheckout(Map<String, Object> body, String userEmail) {
        List<Map<String, Object>> cartItems = extractCartItems(body.get("cart"));
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Sepet boş olamaz!");
        }

        List<Map<String, Object>> sanitizedCart = new ArrayList<>();
        double subTotal = 0.0;
        double couponEligibleTotal = 0.0;
        String couponProductType = null;
        String couponCode = str(body, "couponCode");

        Coupon coupon = resolveCoupon(couponCode, userEmail);
        if (coupon != null) {
            couponProductType = normalizeText(coupon.getProductType());
        }

        for (Map<String, Object> item : cartItems) {
            UUID productId = parseUuid(item.get("_id"), item.get("id"));
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + productId));

            int quantity = parseQuantity(item.get("orderQuantity"));
            int stockQuantity = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
            if (quantity > stockQuantity) {
                throw new RuntimeException(product.getName() + " için yeterli stok yok.");
            }

            double price = product.getPrice() != null ? product.getPrice() : 0.0;
            double originalPrice = product.getOriginalPrice() != null ? product.getOriginalPrice() : price;
            int lineDiscount = 0;
            if (originalPrice > price && originalPrice > 0) {
                lineDiscount = (int) Math.round((1 - price / originalPrice) * 100);
            }

            double lineTotal = price * quantity;
            subTotal += lineTotal;

            if (coupon != null && matchesCouponProductType(couponProductType, product)) {
                couponEligibleTotal += lineTotal;
            }

            Map<String, Object> sanitized = new LinkedHashMap<>();
            sanitized.put("_id", product.getId().toString());
            sanitized.put("title", product.getName());
            sanitized.put("price", formatAmount(price));
            sanitized.put("originalPrice", formatAmount(originalPrice));
            sanitized.put("discount", lineDiscount);
            sanitized.put("orderQuantity", quantity);
            sanitized.put("image", product.getImage());
            sanitized.put("parent", product.getParentCategory());
            sanitized.put("category", Map.of("name", product.getCategoryName() != null ? product.getCategoryName() : ""));
            sanitized.put("brand", Map.of("name", product.getBrandName() != null ? product.getBrandName() : ""));
            sanitized.put("sku", product.getSku());
            sanitizedCart.add(sanitized);
        }

        double shippingCost = Math.max(0.0, toDouble(body.get("shippingCost")));
        double discountAmount = 0.0;

        if (coupon != null) {
            if (isCouponExpired(coupon.getEndTime())) {
                throw new RuntimeException("Kuponun süresi dolmuş.");
            }
            if (subTotal < coupon.getMinimumAmount()) {
                throw new RuntimeException("Kupon için minimum sepet tutarı sağlanmadı.");
            }
            if (couponEligibleTotal <= 0.0) {
                throw new RuntimeException("Kupon sepetteki ürünlere uygulanamıyor.");
            }
            discountAmount = couponEligibleTotal * (coupon.getDiscountPercentage() / 100.0);
        }

        double totalAmount = Math.max(0.0, subTotal + shippingCost - discountAmount);
        return new CheckoutSummary(
                formatAmount(subTotal),
                formatAmount(shippingCost),
                formatAmount(discountAmount),
                formatAmount(totalAmount),
                sanitizedCart,
                coupon != null ? coupon.getCouponCode() : null,
                coupon != null ? coupon.getTitle() : null
        );
    }

    private Coupon resolveCoupon(String couponCode, String userEmail) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }

        Coupon coupon = couponRepository.findByCouponCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new RuntimeException("Geçersiz kupon kodu."));

        String scope = normalizeText(coupon.getScope());
        String normalizedUserEmail = normalizeText(userEmail);
        String assignedUserEmail = normalizeText(coupon.getAssignedUserEmail());

        if ("user".equals(scope) && !normalizedUserEmail.equals(assignedUserEmail)) {
            throw new RuntimeException("Bu kupon yalnızca atanmış müşteri hesabında kullanılabilir.");
        }

        String status = normalizeText(coupon.getStatus());
        if (!status.isBlank() && !"active".equals(status)) {
            throw new RuntimeException("Bu kupon şu anda aktif değil.");
        }

        if (isCouponNotStarted(coupon.getStartTime())) {
            throw new RuntimeException("Bu kupon henüz kullanıma açılmadı.");
        }

        return coupon;
    }

    private List<Map<String, Object>> extractCartItems(Object rawCart) {
        if (rawCart == null) {
            return List.of();
        }
        return objectMapper.convertValue(rawCart, new TypeReference<List<Map<String, Object>>>() {});
    }

    private UUID parseUuid(Object... candidates) {
        for (Object candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            try {
                return UUID.fromString(candidate.toString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        throw new RuntimeException("Ürün kimliği geçersiz.");
    }

    private int parseQuantity(Object rawQuantity) {
        int quantity = 1;
        if (rawQuantity != null) {
            try {
                quantity = Integer.parseInt(rawQuantity.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        if (quantity <= 0) {
            throw new RuntimeException("Ürün adedi geçersiz.");
        }
        return quantity;
    }

    private boolean matchesCouponProductType(String couponProductType, Product product) {
        if (couponProductType == null || couponProductType.isBlank()) {
            return false;
        }

        String parentCategory = normalizeText(product.getParentCategory());
        String categoryName = normalizeText(product.getCategoryName());
        return couponProductType.equals(parentCategory) || couponProductType.equals(categoryName);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isCouponExpired(String rawEndTime) {
        if (rawEndTime == null || rawEndTime.isBlank()) {
            return false;
        }

        ZoneId zoneId = ZoneId.of("Europe/Istanbul");
        LocalDateTime now = LocalDateTime.now(zoneId);
        try {
            return OffsetDateTime.parse(rawEndTime).atZoneSameInstant(zoneId).toLocalDateTime().isBefore(now);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(rawEndTime).isBefore(now);
        } catch (DateTimeParseException ignored) {
        }

        return false;
    }

    private boolean isCouponNotStarted(String rawStartTime) {
        if (rawStartTime == null || rawStartTime.isBlank()) {
            return false;
        }

        ZoneId zoneId = ZoneId.of("Europe/Istanbul");
        LocalDateTime now = LocalDateTime.now(zoneId);
        try {
            return OffsetDateTime.parse(rawStartTime).atZoneSameInstant(zoneId).toLocalDateTime().isAfter(now);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(rawStartTime).isAfter(now);
        } catch (DateTimeParseException ignored) {
        }

        return false;
    }

    private double formatAmount(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    private record CheckoutSummary(
            double subTotal,
            double shippingCost,
            double discountAmount,
            double totalAmount,
            List<Map<String, Object>> sanitizedCart,
            String couponCode,
            String couponTitle
    ) {}

    // ---------- Dashboard: özet kartlar ----------

    public Map<String, Object> getDashboardAmount() {
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Europe/Istanbul"));
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime monthStart = now.toLocalDate().withDayOfMonth(1).atStartOfDay();

        double todayAmount = sumAmounts(orderRepository.findByCreatedAtBetween(todayStart, todayEnd));
        double yesterdayAmount = sumAmounts(orderRepository.findByCreatedAtBetween(yesterdayStart, todayStart));
        double monthlyAmount = sumAmounts(orderRepository.findByCreatedAtBetween(monthStart, todayEnd));
        double totalAmount = sumAmounts(orderRepository.findAll());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("todayOrderAmount", todayAmount);
        result.put("yesterdayOrderAmount", yesterdayAmount);
        result.put("monthlyOrderAmount", monthlyAmount);
        result.put("totalOrderAmount", totalAmount);
        result.put("todayCardPaymentAmount", 0);
        result.put("todayCashPaymentAmount", 0);
        result.put("yesterDayCardPaymentAmount", 0);
        result.put("yesterDayCashPaymentAmount", 0);
        return result;
    }

    // ---------- Dashboard: son siparişler ----------

    public Map<String, Object> getDashboardRecentOrders() {
        List<Order> all = orderRepository.findAllByOrderByCreatedAtDesc();
        List<Map<String, Object>> orders = all.stream()
                .limit(10)
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("_id", o.getId().toString());
                    m.put("name", o.getName() != null ? o.getName() : "");
                    m.put("totalAmount", o.getTotalAmount() != null ? o.getTotalAmount() : 0.0);
                    m.put("paymentMethod", "");
                    m.put("status", o.getStatus() != null ? o.getStatus() : "pending");
                    m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                    m.put("updatedAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
                    m.put("invoice", o.getInvoice() != null ? o.getInvoice() : "");
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orders", orders);
        result.put("totalOrder", all.size());
        return result;
    }

    // ---------- Dashboard: satış raporu (son 30 gün) ----------

    public Map<String, Object> getSalesReport() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now(java.time.ZoneId.of("Europe/Istanbul")).minusDays(30);
        List<Order> orders = orderRepository.findByCreatedAtAfter(thirtyDaysAgo);

        Map<String, List<Order>> byDate = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate().toString()));

        List<Map<String, Object>> salesReport = byDate.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", e.getKey());
                    m.put("total", sumAmounts(e.getValue()));
                    m.put("order", e.getValue().size());
                    return m;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("date")))
                .collect(Collectors.toList());

        return Map.of("salesReport", salesReport);
    }

    // ---------- Dashboard: en çok satan kategori ----------

    public Map<String, Object> getMostSellingCategory() {
        List<Order> orders = orderRepository.findAll();
        Map<String, Integer> categoryCount = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getCart() == null || order.getCart().isBlank()) continue;
            try {
                List<Map<String, Object>> cartItems = objectMapper.readValue(
                        order.getCart(), new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> item : cartItems) {
                    Object categoryObj = item.get("category");
                    String categoryName = null;
                    if (categoryObj instanceof Map) {
                        Object nameObj = ((Map<?, ?>) categoryObj).get("name");
                        if (nameObj != null) categoryName = nameObj.toString();
                    } else if (categoryObj instanceof String) {
                        categoryName = (String) categoryObj;
                    }
                    if (categoryName != null && !categoryName.isBlank()) {
                        Object qty = item.get("orderQuantity");
                        int quantity = qty != null ? Integer.parseInt(qty.toString()) : 1;
                        categoryCount.merge(categoryName, quantity, Integer::sum);
                    }
                }
            } catch (Exception e) {
                log.warn("Cart JSON parse hatası (orderId={}): {}", order.getId(), e.getMessage());
            }
        }

        List<Map<String, Object>> categoryData = categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("_id", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());

        return Map.of("categoryData", categoryData);
    }

    // ---------- Yardımcı ----------

    private double sumAmounts(List<Order> orders) {
        return orders.stream()
                .mapToDouble(o -> o.getTotalAmount() != null ? o.getTotalAmount() : 0.0)
                .sum();
    }
}
