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
import com.iyzipay.Options;
import com.iyzipay.model.Address;
import com.iyzipay.model.BasketItem;
import com.iyzipay.model.BasketItemType;
import com.iyzipay.model.Buyer;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.model.CheckoutFormInitialize;
import com.iyzipay.model.Currency;
import com.iyzipay.model.PaymentGroup;
import com.iyzipay.request.CreateCheckoutFormInitializeRequest;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
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
    private final Options iyzicoOptions;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ---------- iyzico: Ödeme başlat ----------

    public Map<String, Object> initializePayment(Map<String, Object> body, String email, HttpServletRequest httpRequest) {
        User user = null;
        if (email != null && !email.isBlank() && !"anonymousUser".equals(email)) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        CheckoutSummary cs = calculateCheckout(body, email);

        String conversationId = UUID.randomUUID().toString();

        CreateCheckoutFormInitializeRequest request = new CreateCheckoutFormInitializeRequest();
        request.setLocale("tr");
        request.setConversationId(conversationId);
        request.setPrice(bigDecimal(cs.subTotal() + cs.shippingCost()));
        request.setPaidPrice(bigDecimal(cs.totalAmount()));
        request.setCurrency(Currency.TRY.name());
        request.setBasketId("BASKET-" + conversationId);
        request.setPaymentGroup(PaymentGroup.PRODUCT.name());
        request.setCallbackUrl(frontendUrl + "/api/payment-callback");

        Buyer buyer = new Buyer();
        if (user != null) {
            buyer.setId(user.getId().toString());
            buyer.setName(user.getFirstName() != null ? user.getFirstName() : "Müşteri");
            buyer.setSurname(user.getLastName() != null ? user.getLastName() : "-");
            buyer.setEmail(user.getEmail());
            // Identity number can be zipCode fallback for TR sandbox, or real input
            String identity = user.getZipCode() != null && user.getZipCode().length() >= 11 ? user.getZipCode() : "11111111111";
            buyer.setIdentityNumber(identity);
        } else {
            String guestEmail = str(body, "email");
            if (guestEmail == null || guestEmail.isBlank()) {
                throw new RuntimeException("Misafir siparişi için e-posta adresi gereklidir.");
            }
            buyer.setId("GUEST-" + UUID.randomUUID().toString().substring(0, 8));
            String fullName = str(body, "name") != null ? str(body, "name") : "Misafir Müşteri";
            String[] parts = fullName.split(" ");
            buyer.setName(parts[0]);
            buyer.setSurname(parts.length > 1 ? parts[parts.length - 1] : "-");
            buyer.setEmail(guestEmail);
            
            // For guests, try to get identityNumber from body, else use zipCode, else sandbox fallback
            String gIdentity = str(body, "identityNumber");
            if (gIdentity == null || gIdentity.length() < 11) gIdentity = str(body, "zipCode");
            if (gIdentity == null || gIdentity.length() < 11) gIdentity = "11111111111";
            
            buyer.setIdentityNumber(gIdentity);
        }
        
        String addr = str(body, "address");
        String city = str(body, "city") != null ? str(body, "city") : (user != null && user.getCity() != null ? user.getCity() : "Istanbul");
        String country = str(body, "country") != null ? str(body, "country") : (user != null && user.getCountry() != null ? user.getCountry() : "Turkey");
        
        buyer.setRegistrationAddress(addr != null ? addr + ", " + city : city);
        buyer.setCity(city);
        buyer.setCountry(country);
        
        String clientIp = httpRequest.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) clientIp = httpRequest.getRemoteAddr();
        buyer.setIp(clientIp != null ? clientIp.split(",")[0].trim() : "127.0.0.1");
        
        String phone = str(body, "contact") != null ? str(body, "contact") : (user != null ? user.getPhone() : null);
        buyer.setGsmNumber(formatPhoneForIyzico(phone != null ? phone : "+905555555555"));
        request.setBuyer(buyer);

        Address address = new Address();
        address.setContactName(str(body, "name") != null ? str(body, "name") : buyer.getName() + " " + buyer.getSurname());
        address.setCity(city);
        address.setCountry(country);
        address.setAddress(addr);
        request.setShippingAddress(address);
        request.setBillingAddress(address);

        List<BasketItem> basketItems = new ArrayList<>();
        for (Map<String, Object> item : cs.sanitizedCart()) {
            BasketItem bi = new BasketItem();
            bi.setId(str(item, "_id"));
            bi.setName(str(item, "title"));
            bi.setCategory1(str(item, "parent") != null ? str(item, "parent") : "Genel");
            bi.setItemType(BasketItemType.PHYSICAL.name());
            double price = toDouble(item.get("price"));
            int qty = item.get("orderQuantity") instanceof Number
                    ? ((Number) item.get("orderQuantity")).intValue()
                    : Integer.parseInt(item.get("orderQuantity").toString());
            bi.setPrice(bigDecimal(price * qty));
            basketItems.add(bi);
        }
        if (cs.shippingCost() > 0) {
            BasketItem shipping = new BasketItem();
            shipping.setId("SHIPPING");
            shipping.setName("Kargo");
            shipping.setCategory1("Kargo");
            shipping.setItemType(BasketItemType.PHYSICAL.name());
            shipping.setPrice(bigDecimal(cs.shippingCost()));
            basketItems.add(shipping);
        }
        request.setBasketItems(basketItems);

        CheckoutFormInitialize result = CheckoutFormInitialize.create(request, iyzicoOptions);

        if (!"success".equals(result.getStatus())) {
            log.error("iyzico init failed [{}]: {}", result.getErrorCode(), result.getErrorMessage());
            throw new RuntimeException("Ödeme başlatılamadı: " + result.getErrorMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("checkoutFormContent", result.getCheckoutFormContent());
        response.put("paymentPageUrl", result.getPaymentPageUrl());
        response.put("token", result.getToken());
        response.put("conversationId", conversationId);
        response.put("subTotal", formatAmount(cs.subTotal()));
        response.put("shippingCost", formatAmount(cs.shippingCost()));
        response.put("discount", formatAmount(cs.discountAmount()));
        response.put("totalAmount", formatAmount(cs.totalAmount()));
        return response;
    }

    // ---------- iyzico: Ödeme doğrula ve siparişi kaydet ----------

    @Transactional
    public Map<String, Object> confirmPayment(Map<String, Object> body, String email) {
        String token = str(body, "token");
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Ödeme token'ı eksik.");
        }

        // Idempotency: aynı token ile tekrar sipariş oluşturma
        Optional<Order> existing = orderRepository.findByIyzicoToken(token);
        if (existing.isPresent()) {
            Order ex = existing.get();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("success", true);
            resp.put("orderId", ex.getId().toString());
            resp.put("order", toResponse(ex));
            return resp;
        }

        String conversationId = str(body, "conversationId");
        RetrieveCheckoutFormRequest retrieveRequest = new RetrieveCheckoutFormRequest();
        retrieveRequest.setLocale("tr");
        retrieveRequest.setConversationId(conversationId);
        retrieveRequest.setToken(token);

        CheckoutForm checkoutForm = CheckoutForm.retrieve(retrieveRequest, iyzicoOptions);

        if (!"success".equals(checkoutForm.getStatus()) || !"SUCCESS".equals(checkoutForm.getPaymentStatus())) {
            log.error("iyzico payment failed [{}]: {} (Token: {}, ConversationId: {})", 
                      checkoutForm.getErrorCode(), checkoutForm.getErrorMessage(), token, conversationId);
            throw new RuntimeException("Ödeme doğrulanamadı: " +
                    (checkoutForm.getErrorMessage() != null ? checkoutForm.getErrorMessage() : checkoutForm.getPaymentStatus()));
        }

        if (checkoutForm.getConversationId() == null || !checkoutForm.getConversationId().equals(conversationId)) {
            log.error("iyzico conversationId mismatch! Expected: {}, Received: {} (Token: {})", 
                      conversationId, checkoutForm.getConversationId(), token);
            throw new RuntimeException("Ödeme doğrulanamadı: Güvenlik ihlali (Geçersiz ConversationId).");
        }

        // paidPrice cross-check: iyzico'dan dönen tutarla backend hesabını karşılaştır
        User user = null;
        if (email != null && !email.isBlank() && !"anonymousUser".equals(email)) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        
        CheckoutSummary cs = calculateCheckout(body, email);

        if (checkoutForm.getPaidPrice() != null) {
            double iyzicoAmount = checkoutForm.getPaidPrice().doubleValue();
            double expectedAmount = cs.totalAmount();
            if (Math.abs(iyzicoAmount - expectedAmount) > 0.02) {
                log.error("paidPrice mismatch! iyzico: {}, expected: {} (Token: {})", iyzicoAmount, expectedAmount, token);
                throw new RuntimeException("Ödeme tutarı uyuşmazlığı tespit edildi.");
            }
        }

        log.info("iyzico payment verified successfully. Token: {}, ConversationId: {}, Amount: {}", token, conversationId, cs.totalAmount());

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
        
        if (user != null) {
            order.setUserId(user.getId().toString());
            order.setIsGuest(false);
        } else {
            order.setIsGuest(true);
            order.setGuestEmail(str(body, "email"));
            order.setGuestName(str(body, "name"));
            order.setGuestPhone(str(body, "contact"));
        }

        order.setSubTotal(cs.subTotal());
        order.setShippingCost(cs.shippingCost());
        order.setDiscount(cs.discountAmount());
        order.setTotalAmount(cs.totalAmount());
        order.setCouponCode(cs.couponCode());
        order.setCouponTitle(cs.couponTitle());

        order.setCart(toJson(cs.sanitizedCart()));
        order.setIyzicoToken(token);
        order.setIyzicoConversationId(checkoutForm.getConversationId());
        order.setIyzicoPaymentId(checkoutForm.getPaymentId());
        order.setIyzicoPaymentDetail(toJson(Map.of(
                "paymentId", checkoutForm.getPaymentId() != null ? checkoutForm.getPaymentId() : "",
                "conversationId", checkoutForm.getConversationId() != null ? checkoutForm.getConversationId() : "",
                "paymentStatus", checkoutForm.getPaymentStatus() != null ? checkoutForm.getPaymentStatus() : "",
                "price", checkoutForm.getPrice() != null ? checkoutForm.getPrice().toString() : "",
                "paidPrice", checkoutForm.getPaidPrice() != null ? checkoutForm.getPaidPrice().toString() : ""
        )));

        Order saved;
        try {
            saved = orderRepository.save(order);
        } catch (DataIntegrityViolationException e) {
            // Race condition: başka bir thread aynı token ile siparişi zaten kaydetmiş
            log.warn("Duplicate iyzicoToken race condition caught for token: {}", token);
            Optional<Order> duplicate = orderRepository.findByIyzicoToken(token);
            if (duplicate.isPresent()) {
                Order ex = duplicate.get();
                Map<String, Object> resp = new LinkedHashMap<>();
                resp.put("success", true);
                resp.put("orderId", ex.getId().toString());
                resp.put("order", toResponse(ex));
                return resp;
            }
            throw new RuntimeException("Sipariş kaydedilemedi. Lütfen tekrar deneyin.");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("orderId", saved.getId().toString());
        response.put("order", toResponse(saved));
        return response;
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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı!"));

        // If authenticated, check ownership
        if (email != null) {
            if (!order.getEmail().equals(email) && !order.getUserId().equals(email)) {
                throw new RuntimeException("Bu siparişi görüntüleme yetkiniz yok.");
            }
        } 
        // If guest, we allow viewing by UUID (Security by Obscurity via non-enumerable IDs)
        
        return Map.of("order", toResponse(order));
    }

    public Map<String, Object> getOrderByInvoiceAndEmail(String invoice, String email) {
        Order order = orderRepository.findByInvoiceAndEmail(invoice, email)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı veya e-posta eşleşmiyor."));
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
        map.put("isGuest", o.getIsGuest() != null && o.getIsGuest());
        map.put("guestEmail", o.getGuestEmail());
        map.put("invoice", o.getInvoice());
        map.put("subTotal", o.getSubTotal());
        map.put("shippingCost", o.getShippingCost());
        map.put("discount", o.getDiscount());
        map.put("totalAmount", o.getTotalAmount());
        map.put("couponCode", o.getCouponCode());
        map.put("couponTitle", o.getCouponTitle());
        map.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);
        map.put("cart", fromJson(o.getCart()));
        map.put("reviewedProducts", fromJson(o.getReviewedProducts()));
        map.put("iyzicoToken", o.getIyzicoToken());
        map.put("iyzicoPaymentId", o.getIyzicoPaymentId());
        map.put("iyzicoConversationId", o.getIyzicoConversationId());
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

        if ("user".equals(scope)) {
            if (normalizedUserEmail.isEmpty() || "anonymoususer".equals(normalizedUserEmail)) {
                throw new RuntimeException("Bu kuponu kullanmak için giriş yapmalısınız.");
            }
            if (!normalizedUserEmail.equals(assignedUserEmail)) {
                throw new RuntimeException("Bu kupon yalnızca atanmış müşteri hesabında kullanılabilir.");
            }
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

    private BigDecimal bigDecimal(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
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
                    String displayName = o.getName();
                    if ((displayName == null || displayName.isBlank()) && o.getIsGuest() != null && o.getIsGuest()) {
                        displayName = o.getGuestName() != null ? o.getGuestName() : "Misafir";
                    }
                    m.put("name", displayName != null ? displayName : "");
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

    private String formatPhoneForIyzico(String phone) {
        if (phone == null || phone.isBlank()) return "+905555555555";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 10) return "+905555555555";
        if (digits.startsWith("0")) digits = digits.substring(1);
        if (digits.startsWith("90")) digits = digits.substring(2);
        return "+90" + digits;
    }
}
