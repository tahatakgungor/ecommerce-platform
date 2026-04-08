package com.ecommerce.product.application;

import com.ecommerce.product.domain.Coupon;
import com.ecommerce.product.domain.Order;
import com.ecommerce.product.domain.Product;
import com.ecommerce.product.domain.User;
import com.ecommerce.product.repository.CouponRepository;
import com.ecommerce.product.repository.OrderRepository;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iyzipay.Options;
import com.iyzipay.model.CheckoutForm;
import com.iyzipay.request.RetrieveCheckoutFormRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private Options iyzicoOptions;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                userRepository,
                productRepository,
                couponRepository,
                new ObjectMapper(),
                iyzicoOptions,
                eventPublisher
        );
    }

    @Test
    void confirmPayment_withAssignedUserCoupon_shouldApplyDiscountAndPersistCouponInfo() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail("customer@mail.com");

        Product product = new Product();
        product.setId(productId);
        product.setName("Detoks Ürünü");
        product.setPrice(200.0);
        product.setOriginalPrice(240.0);
        product.setStockQuantity(10);
        product.setParentCategory("Detoks");
        product.setCategoryName("Detoks");

        Coupon coupon = new Coupon();
        coupon.setCouponCode("VIP20");
        coupon.setTitle("VIP 20");
        coupon.setScope("USER");
        coupon.setAssignedUserEmail("customer@mail.com");
        coupon.setStatus("Active");
        coupon.setProductType("Detoks");
        coupon.setDiscountPercentage(20.0);
        coupon.setMinimumAmount(100.0);
        coupon.setEndTime("2099-12-31T23:59:59+03:00");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", "iyzico-test-token");
        body.put("conversationId", "test-conversation-id");
        body.put("name", "Customer");
        body.put("address", "Address 1");
        body.put("contact", "5551234567");
        body.put("email", "customer@mail.com");
        body.put("city", "Istanbul");
        body.put("country", "TR");
        body.put("zipCode", "34000");
        body.put("shippingOption", "standard");
        body.put("shippingCost", 30);
        body.put("couponCode", "VIP20");
        body.put("cart", List.of(Map.of("_id", productId.toString(), "orderQuantity", 2)));

        when(userRepository.findByEmail("customer@mail.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(couponRepository.findByCouponCodeIgnoreCase("VIP20")).thenReturn(Optional.of(coupon));
        when(orderRepository.findTopByIyzicoConversationIdOrderByCreatedAtDesc("test-conversation-id")).thenReturn(Optional.empty());
        when(orderRepository.findTopByIyzicoTokenOrderByCreatedAtDesc("iyzico-test-token")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(UUID.randomUUID());
            return order;
        });

        CheckoutForm mockForm = mock(CheckoutForm.class);
        when(mockForm.getStatus()).thenReturn("success");
        when(mockForm.getPaymentStatus()).thenReturn("SUCCESS");
        when(mockForm.getPaymentId()).thenReturn("PAY123");
        when(mockForm.getConversationId()).thenReturn("test-conversation-id");
        when(mockForm.getPrice()).thenReturn(new BigDecimal("430.00"));
        when(mockForm.getPaidPrice()).thenReturn(new BigDecimal("350.00"));

        try (MockedStatic<CheckoutForm> mockedForm = mockStatic(CheckoutForm.class)) {
            mockedForm.when(() -> CheckoutForm.retrieve(any(RetrieveCheckoutFormRequest.class), any(Options.class)))
                    .thenReturn(mockForm);

            Map<String, Object> result = orderService.confirmPayment(body, "customer@mail.com");

            @SuppressWarnings("unchecked")
            Map<String, Object> orderMap = (Map<String, Object>) result.get("order");

            assertEquals(true, result.get("success"));
            assertEquals("VIP20", orderMap.get("couponCode"));
            assertEquals("VIP 20", orderMap.get("couponTitle"));
            assertEquals(400.0, orderMap.get("subTotal"));
            assertEquals(80.0, orderMap.get("discount"));
            assertEquals(350.0, orderMap.get("totalAmount"));

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(captor.capture());
            Order savedOrder = captor.getValue();
            assertEquals("VIP20", savedOrder.getCouponCode());
            assertEquals("VIP 20", savedOrder.getCouponTitle());
            assertEquals(80.0, savedOrder.getDiscount());
        }
    }

    @Test
    void confirmPayment_withCouponAssignedToAnotherUser_shouldThrow() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setEmail("another@mail.com");

        Product product = new Product();
        product.setId(productId);
        product.setName("Detoks Ürünü");
        product.setPrice(200.0);
        product.setOriginalPrice(200.0);
        product.setStockQuantity(10);
        product.setParentCategory("Detoks");
        product.setCategoryName("Detoks");

        Coupon coupon = new Coupon();
        coupon.setCouponCode("VIP20");
        coupon.setTitle("VIP 20");
        coupon.setScope("USER");
        coupon.setAssignedUserEmail("customer@mail.com");
        coupon.setStatus("Active");
        coupon.setProductType("Detoks");
        coupon.setDiscountPercentage(20.0);
        coupon.setMinimumAmount(100.0);
        coupon.setEndTime("2099-12-31T23:59:59+03:00");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", "iyzico-test-token-2");
        body.put("conversationId", "test-conv-2");
        body.put("name", "Another Customer");
        body.put("address", "Address 2");
        body.put("contact", "5550000000");
        body.put("email", "another@mail.com");
        body.put("city", "Istanbul");
        body.put("country", "TR");
        body.put("zipCode", "34000");
        body.put("shippingOption", "standard");
        body.put("shippingCost", 30);
        body.put("couponCode", "VIP20");
        body.put("cart", List.of(Map.of("_id", productId.toString(), "orderQuantity", 1)));

        when(orderRepository.findTopByIyzicoConversationIdOrderByCreatedAtDesc("test-conv-2")).thenReturn(Optional.empty());
        when(orderRepository.findTopByIyzicoTokenOrderByCreatedAtDesc("iyzico-test-token-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("another@mail.com")).thenReturn(Optional.of(user));
        when(couponRepository.findByCouponCodeIgnoreCase("VIP20")).thenReturn(Optional.of(coupon));

        CheckoutForm mockForm = mock(CheckoutForm.class);
        when(mockForm.getStatus()).thenReturn("success");
        when(mockForm.getPaymentStatus()).thenReturn("SUCCESS");
        when(mockForm.getPaymentId()).thenReturn("PAY456");
        when(mockForm.getConversationId()).thenReturn("test-conv-2");
        when(mockForm.getPrice()).thenReturn(new BigDecimal("230.00"));
        when(mockForm.getPaidPrice()).thenReturn(new BigDecimal("230.00"));

        try (MockedStatic<CheckoutForm> mockedForm = mockStatic(CheckoutForm.class)) {
            mockedForm.when(() -> CheckoutForm.retrieve(any(RetrieveCheckoutFormRequest.class), any(Options.class)))
                    .thenReturn(mockForm);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> orderService.confirmPayment(body, "another@mail.com"));
            assertEquals("Bu kupon yalnızca atanmış müşteri hesabında kullanılabilir.", ex.getMessage());
        }
    }

    @Test
    void confirmPayment_withConversationIdMismatch_shouldThrow() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", "iyzico-test-token-mismatch");
        body.put("conversationId", "expected-conv-id");
        body.put("cart", List.of()); // Just to bypass early null checks if any, though token comes first.

        when(orderRepository.findTopByIyzicoConversationIdOrderByCreatedAtDesc("expected-conv-id")).thenReturn(Optional.empty());
        when(orderRepository.findTopByIyzicoTokenOrderByCreatedAtDesc("iyzico-test-token-mismatch")).thenReturn(Optional.empty());

        CheckoutForm mockForm = mock(CheckoutForm.class);
        when(mockForm.getStatus()).thenReturn("success");
        when(mockForm.getPaymentStatus()).thenReturn("SUCCESS");
        when(mockForm.getConversationId()).thenReturn("different-conv-id"); // Mismatch here

        try (MockedStatic<CheckoutForm> mockedForm = mockStatic(CheckoutForm.class)) {
            mockedForm.when(() -> CheckoutForm.retrieve(any(RetrieveCheckoutFormRequest.class), any(Options.class)))
                    .thenReturn(mockForm);

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> orderService.confirmPayment(body, "test@mail.com"));
            assertEquals("Ödeme doğrulanamadı: Güvenlik ihlali (Geçersiz ConversationId).", ex.getMessage());
        }
    }
}
