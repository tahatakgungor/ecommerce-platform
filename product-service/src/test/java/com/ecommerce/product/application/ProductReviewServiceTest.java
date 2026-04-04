package com.ecommerce.product.application;

import com.ecommerce.product.domain.*;
import com.ecommerce.product.dto.review.ReviewCreateRequest;
import com.ecommerce.product.dto.review.ReviewResponse;
import com.ecommerce.product.dto.review.ReviewSummaryResponse;
import com.ecommerce.product.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository productReviewRepository;

    @Mock
    private ProductReviewFeedbackRepository productReviewFeedbackRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private FileUploadService fileUploadService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private ProductReviewService productReviewService;

    @Test
    void createReview_shouldRejectWhenUserHasNoDeliveredOrder() {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");

        when(productRepository.findById(productId)).thenReturn(Optional.of(new Product()));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of());

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setCommentBody("Harika ürün gerçekten");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productReviewService.createReview(productId, request, "customer@example.com"));

        assertEquals("Yorum yapabilmek için ürün siparişinizin teslim edilmiş olması gerekir.", ex.getMessage());
    }

    @Test
    void createReview_shouldRejectWhenOrderIsNotDeliveredEvenIfProductExistsInCart() {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");
        Product product = new Product();
        product.setId(productId);

        Order processingOrder = new Order();
        processingOrder.setStatus("processing");
        processingOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(processingOrder));

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setCommentBody("Harika ürün gerçekten");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productReviewService.createReview(productId, request, "customer@example.com"));

        assertEquals("Yorum yapabilmek için ürün siparişinizin teslim edilmiş olması gerekir.", ex.getMessage());
    }

    @Test
    void createReview_shouldRejectWhenDeliveredOrderDoesNotContainProduct() {
        UUID productId = UUID.randomUUID();
        UUID anotherProductId = UUID.randomUUID();
        User user = customer("customer@example.com");
        Product product = new Product();
        product.setId(productId);

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + anotherProductId + "\",\"orderQuantity\":1}]");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(deliveredOrder));

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setCommentBody("Harika ürün gerçekten");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productReviewService.createReview(productId, request, "customer@example.com"));

        assertEquals("Yorum yapabilmek için ürün siparişinizin teslim edilmiş olması gerekir.", ex.getMessage());
    }

    @Test
    void createReview_shouldRejectDuplicateReview() throws Exception {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");
        Product product = new Product();
        product.setId(productId);

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(deliveredOrder));
        when(productReviewRepository.findByProductIdAndUserId(productId, user.getId())).thenReturn(Optional.of(new ProductReview()));
        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setCommentBody("Çok iyi");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> productReviewService.createReview(productId, request, "customer@example.com"));

        assertEquals("Bu ürün için zaten bir yorumunuz var. Düzenleme yapabilirsiniz.", ex.getMessage());
    }

    @Test
    void createReview_shouldCreatePendingVerifiedReviewForCleanContent() throws Exception {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");
        user.setName("Ali Veli");
        Product product = new Product();
        product.setId(productId);

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(deliveredOrder));
        when(productReviewRepository.findByProductIdAndUserId(productId, user.getId())).thenReturn(Optional.empty());
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(4);
        request.setCommentTitle("Memnun kaldım");
        request.setCommentBody("Ürün beklentimi karşıladı");

        productReviewService.createReview(productId, request, "customer@example.com");

        ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
        verify(productReviewRepository).save(captor.capture());

        ProductReview saved = captor.getValue();
        assertTrue(saved.isVerifiedPurchase());
        assertEquals(ReviewStatus.PENDING, saved.getStatus());
        assertEquals(4, saved.getRating());
    }

    @Test
    void createReview_shouldAllowNewOrderAndMarkReviewedProductsWhenOrderIdProvided() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        User user = customer("customer@example.com");
        Product product = new Product();
        product.setId(productId);

        ProductReview existing = new ProductReview();
        existing.setId(UUID.randomUUID());
        existing.setProduct(product);
        existing.setUser(user);
        existing.setRating(3);
        existing.setCommentBody("Eski yorum");
        existing.setHelpfulCount(7L);
        existing.setNotHelpfulCount(2L);

        Order deliveredOrder = new Order();
        deliveredOrder.setId(orderId);
        deliveredOrder.setUserId(user.getId().toString());
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");
        deliveredOrder.setReviewedProducts("[]");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(deliveredOrder));
        when(productReviewRepository.findByProductIdAndUserId(productId, user.getId())).thenReturn(Optional.of(existing));
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setOrderId(orderId);
        request.setRating(5);
        request.setCommentBody("Yeni sipariş için tekrar yorum.");

        ReviewResponse response = productReviewService.createReview(productId, request, "customer@example.com");

        assertNotNull(response);
        verify(orderRepository).save(any(Order.class));
        assertTrue(deliveredOrder.getReviewedProducts().contains(productId.toString()));
        assertEquals(5, existing.getRating());
        assertEquals(0L, existing.getHelpfulCount());
        assertEquals(0L, existing.getNotHelpfulCount());
    }

    @Test
    void createReview_shouldRejectBlockedWordContent() {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");
        user.setName("Ali Veli");
        Product product = new Product();
        product.setId(productId);

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(deliveredOrder));
        when(productReviewRepository.findByProductIdAndUserId(productId, user.getId())).thenReturn(Optional.empty());
        when(productReviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> {
            ProductReview review = invocation.getArgument(0);
            review.setId(UUID.randomUUID());
            return review;
        });

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(1);
        request.setCommentBody("Bu içerik casino kelimesi içeriyor.");

        productReviewService.createReview(productId, request, "customer@example.com");

        ArgumentCaptor<ProductReview> captor = ArgumentCaptor.forClass(ProductReview.class);
        verify(productReviewRepository).save(captor.capture());
        ProductReview saved = captor.getValue();
        assertEquals(ReviewStatus.REJECTED, saved.getStatus());
    }

    @Test
    void getReviewSummary_shouldReturnAverageAndDistribution() {
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productReviewRepository.findAverageAndCountByProductAndStatus(productId, ReviewStatus.APPROVED))
                .thenReturn(List.<Object[]>of(new Object[]{4.25d, 20L}));
        when(productReviewRepository.findRatingDistributionByProductAndStatus(productId, ReviewStatus.APPROVED))
                .thenReturn(List.of(
                        new Object[]{5, 12L},
                        new Object[]{4, 5L},
                        new Object[]{3, 2L},
                        new Object[]{1, 1L}
                ));

        ReviewSummaryResponse summary = productReviewService.getReviewSummary(productId);

        assertEquals(4.25d, summary.getAverageRating());
        assertEquals(20L, summary.getTotalReviews());
        assertEquals(12L, summary.getStarCounts().get(5));
        assertEquals(60.0d, summary.getStarPercentages().get(5));
        assertEquals(0L, summary.getStarCounts().get(2));
    }

    @Test
    void getReviewSummary_shouldNotFailOnNestedAggregateRow() {
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(true);
        when(productReviewRepository.findAverageAndCountByProductAndStatus(productId, ReviewStatus.APPROVED))
                .thenReturn(List.<Object[]>of(new Object[]{new Object[]{4.67d, 15L}}));
        when(productReviewRepository.findRatingDistributionByProductAndStatus(productId, ReviewStatus.APPROVED))
                .thenReturn(List.of(new Object[]{5, 12L}, new Object[]{4, 3L}));

        ReviewSummaryResponse summary = productReviewService.getReviewSummary(productId);

        assertEquals(4.67d, summary.getAverageRating());
        assertEquals(15L, summary.getTotalReviews());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getApprovedReviews_shouldMaskReviewerName() {
        UUID productId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);

        User reviewer = customer("reviewer@example.com");
        reviewer.setName("Taha Turan Akgüngör");

        ProductReview review = new ProductReview();
        review.setId(UUID.randomUUID());
        review.setProduct(product);
        review.setUser(reviewer);
        review.setStatus(ReviewStatus.APPROVED);
        review.setRating(5);
        review.setCommentBody("Çok iyi ürün.");

        when(productRepository.existsById(productId)).thenReturn(true);
        when(productReviewRepository.findByProductIdAndStatus(any(UUID.class), any(ReviewStatus.class), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(review)));
        when(productReviewRepository.findAverageAndCountByProductAndStatus(productId, ReviewStatus.APPROVED))
                .thenReturn(List.<Object[]>of(new Object[]{5.0d, 1L}));
        when(productReviewRepository.findRatingDistributionByProductAndStatus(productId, ReviewStatus.APPROVED))
                .thenReturn(List.<Object[]>of(new Object[]{5, 1L}));

        Map<String, Object> result = productReviewService.getApprovedReviews(productId, "newest", false, 0, 8);
        List<?> reviews = (List<?>) result.get("reviews");
        assertEquals(1, reviews.size());
        ReviewResponse response = (ReviewResponse) reviews.get(0);
        assertEquals("T*** T*** A***", response.getUserName());
    }

    @Test
    void voteReview_shouldSwitchVoteTypeAndUpdateCounters() {
        UUID productId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        User reviewer = customer("reviewer@example.com");
        User voter = customer("voter@example.com");

        Product product = new Product();
        product.setId(productId);

        ProductReview review = new ProductReview();
        review.setId(reviewId);
        review.setProduct(product);
        review.setUser(reviewer);
        review.setStatus(ReviewStatus.APPROVED);
        review.setHelpfulCount(1L);
        review.setNotHelpfulCount(0L);

        ProductReviewFeedback existing = new ProductReviewFeedback();
        existing.setReview(review);
        existing.setUser(voter);
        existing.setVoteType(ReviewVoteType.HELPFUL);

        when(userRepository.findByEmail("voter@example.com")).thenReturn(Optional.of(voter));
        when(productReviewRepository.findByIdAndProductId(reviewId, productId)).thenReturn(Optional.of(review));
        when(productReviewFeedbackRepository.findByReviewIdAndUserId(reviewId, voter.getId())).thenReturn(Optional.of(existing));

        Map<String, Object> result = productReviewService.voteReview(productId, reviewId, false, "voter@example.com");

        assertEquals(0L, result.get("helpfulCount"));
        assertEquals(1L, result.get("notHelpfulCount"));
        assertEquals(ReviewVoteType.NOT_HELPFUL, existing.getVoteType());
    }

    @Test
    void getReviewEligibility_shouldReturnEligibleForDeliveredCustomerWithoutReview() {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");

        when(productRepository.existsById(productId)).thenReturn(true);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(deliveredOrder));
        when(productReviewRepository.findByProductIdAndUserId(productId, user.getId())).thenReturn(Optional.empty());

        var eligibility = productReviewService.getReviewEligibility(productId, "customer@example.com");

        assertTrue(eligibility.isCanReview());
        assertTrue(eligibility.isDeliveredPurchase());
        assertFalse(eligibility.isAlreadyReviewed());
        assertEquals(null, eligibility.getReason());
    }

    @Test
    void getReviewEligibility_shouldReturnNotEligibleWhenAlreadyReviewed() {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");

        when(productRepository.existsById(productId)).thenReturn(true);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(deliveredOrder));
        when(productReviewRepository.findByProductIdAndUserId(productId, user.getId())).thenReturn(Optional.of(new ProductReview()));

        var eligibility = productReviewService.getReviewEligibility(productId, "customer@example.com");

        assertFalse(eligibility.isCanReview());
        assertTrue(eligibility.isDeliveredPurchase());
        assertTrue(eligibility.isAlreadyReviewed());
        assertEquals("Bu ürün için zaten bir yorumunuz var. Düzenleme yapabilirsiniz.", eligibility.getReason());
    }

    @Test
    void getReviewEligibility_shouldAllowLegacyUserRoleForDeliveredPurchase() {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");
        user.setRole("User");

        Order deliveredOrder = new Order();
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");

        when(productRepository.existsById(productId)).thenReturn(true);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId().toString())).thenReturn(List.of(deliveredOrder));
        when(productReviewRepository.findByProductIdAndUserId(productId, user.getId())).thenReturn(Optional.empty());

        var eligibility = productReviewService.getReviewEligibility(productId, "customer@example.com");

        assertTrue(eligibility.isCanReview());
        assertEquals(null, eligibility.getReason());
    }

    @Test
    void getReviewEligibility_shouldBeOrderScopedWhenOrderIdProvided() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        User user = customer("customer@example.com");

        Order deliveredOrder = new Order();
        deliveredOrder.setId(orderId);
        deliveredOrder.setUserId(user.getId().toString());
        deliveredOrder.setStatus("delivered");
        deliveredOrder.setCart("[{\"_id\":\"" + productId + "\",\"orderQuantity\":1}]");
        deliveredOrder.setReviewedProducts("[]");

        when(productRepository.existsById(productId)).thenReturn(true);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(deliveredOrder));

        var eligibility = productReviewService.getReviewEligibility(productId, orderId, "customer@example.com");

        assertTrue(eligibility.isCanReview());
        assertTrue(eligibility.isDeliveredPurchase());
        assertFalse(eligibility.isAlreadyReviewed());
    }

    @Test
    void uploadReviewMedia_shouldAllowImageForCustomerRole() {
        UUID productId = UUID.randomUUID();
        User user = customer("customer@example.com");
        MockMultipartFile file = new MockMultipartFile("file", "review.jpg", "image/jpeg", "img".getBytes());

        when(productRepository.existsById(productId)).thenReturn(true);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(fileUploadService.saveFile(file)).thenReturn("https://cdn.example.com/review.jpg");

        String url = productReviewService.uploadReviewMedia(productId, file, "customer@example.com");
        assertEquals("https://cdn.example.com/review.jpg", url);
    }

    private User customer(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole("Customer");
        user.setName("Test User");
        return user;
    }
}
