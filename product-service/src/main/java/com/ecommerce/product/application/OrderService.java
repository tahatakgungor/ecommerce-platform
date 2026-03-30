package com.ecommerce.product.application;

import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.OrderRepository;
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

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
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

    public Map<String, String> createPaymentIntent(int price) {
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) price * 100L)   // dolar → sent
                    .setCurrency("usd")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            return Map.of("clientSecret", intent.getClientSecret());
        } catch (Exception e) {
            log.error("Stripe PaymentIntent oluşturulamadı: {}", e.getMessage());
            throw new RuntimeException("Ödeme başlatılamadı: " + e.getMessage());
        }
    }

    // ---------- Sipariş kaydet ----------

    @Transactional
    public Map<String, Object> addOrder(Map<String, Object> body) {
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
        order.setUserId(str(body, "user"));

        order.setSubTotal(toDouble(body.get("subTotal")));
        order.setShippingCost(toDouble(body.get("shippingCost")));
        order.setDiscount(toDouble(body.get("discount")));
        order.setTotalAmount(toDouble(body.get("totalAmount")));

        // Cart, cardInfo, paymentIntent → JSON string olarak sakla
        order.setCart(toJson(body.get("cart")));
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
}
