package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.ReviewRequest;
import com.mk.ecommerce.dto.ReviewResponse;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.entity.Product;
import com.mk.ecommerce.entity.ProductReview;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CustomerRepository;
import com.mk.ecommerce.repository.ProductRepository;
import com.mk.ecommerce.repository.ProductReviewRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private ProductReviewService reviewService;

    @Test
    void should_create_review_when_product_and_customer_exist() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(reviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReviewResponse result = reviewService.createReview(10L, request(4, "Great mouse"));

        // Assert
        assertThat(result.getRating()).isEqualTo(4);
        assertThat(result.getComment()).isEqualTo("Great mouse");
        assertThat(result.getProductId()).isEqualTo(10L);
        assertThat(result.getCustomerName()).isEqualTo("John Doe");
    }

    @Test
    void should_create_review_when_comment_is_omitted() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(reviewRepository.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        ReviewResponse result = reviewService.createReview(10L, request(5, null));

        // Assert
        assertThat(result.getComment()).isNull();
    }

    @Test
    void should_throw_not_found_when_reviewing_missing_product() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(10L, request(4, "Great")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    void should_throw_not_found_when_reviewing_as_missing_customer() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reviewService.createReview(10L, request(4, "Great")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    void should_list_reviews_of_product_when_product_exists() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.of(product()));
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(review(3, "Okay"), review(5, "Perfect")));

        // Act
        List<ReviewResponse> result = reviewService.listReviews(10L);

        // Assert
        assertThat(result).extracting(ReviewResponse::getRating).containsExactly(3, 5);
    }

    @Test
    void should_throw_not_found_when_listing_reviews_of_missing_product() {
        // Arrange
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reviewService.listReviews(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Product product() {
        return Product.builder().id(10L).name("Wireless Mouse").sku("ELEC-MOU-001").build();
    }

    private Customer customer() {
        return Customer.builder().id(1L).firstName("John").lastName("Doe").email("john@example.com").build();
    }

    private ProductReview review(int rating, String comment) {
        return ProductReview.builder()
                .id((long) rating)
                .product(product())
                .customer(customer())
                .rating(rating)
                .comment(comment)
                .build();
    }

    private ReviewRequest request(int rating, String comment) {
        return ReviewRequest.builder().customerId(1L).rating(rating).comment(comment).build();
    }
}
