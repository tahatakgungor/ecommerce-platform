package com.ecommerce.product.application;

import com.ecommerce.product.domain.*;
import com.ecommerce.product.dto.review.ReviewCreateRequest;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void createReview_shouldCreatePendingVerifiedReview() throws Exception {
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

    private User customer(String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setRole("Customer");
        user.setName("Test User");
        return user;
    }
}
