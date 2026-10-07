package com.mk.ecommerce.controller;

import com.mk.ecommerce.IntegrationTestSupport;
import com.mk.ecommerce.dto.ErrorResponse;
import com.mk.ecommerce.dto.ReviewRequest;
import com.mk.ecommerce.dto.ReviewResponse;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class ProductReviewControllerIntegrationTest extends IntegrationTestSupport {

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUpData() {
        product = saveProduct(saveCategory("Electronics", "electronics"), "ELEC-001", "24.99", 10);
        customer = saveCustomer("john.doe@example.com");
    }

    @Test
    void should_create_review_when_request_is_valid() {
        // Arrange
        ReviewRequest request = request(5, "Excellent");

        // Act
        ResponseEntity<ReviewResponse> response = restTemplate.postForEntity(
                "/api/v1/products/{id}/reviews", request, ReviewResponse.class, product.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getRating()).isEqualTo(5);
        assertThat(response.getBody().getCustomerName()).isEqualTo("John Doe");
        assertThat(reviewRepository.count()).isEqualTo(1);
    }

    @Test
    void should_list_reviews_newest_first_when_product_has_reviews() {
        // Arrange
        restTemplate.postForEntity("/api/v1/products/{id}/reviews", request(3, "Okay"),
                ReviewResponse.class, product.getId());
        restTemplate.postForEntity("/api/v1/products/{id}/reviews", request(5, "Perfect"),
                ReviewResponse.class, product.getId());

        // Act
        ResponseEntity<ReviewResponse[]> response = restTemplate.getForEntity(
                "/api/v1/products/{id}/reviews", ReviewResponse[].class, product.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(ReviewResponse::getComment).containsExactly("Perfect", "Okay");
    }

    @Test
    void should_return_bad_request_with_field_error_when_rating_is_out_of_range() {
        // Arrange
        ReviewRequest request = request(6, "Too good");

        // Act
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/products/{id}/reviews", request, ErrorResponse.class, product.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getFieldErrors())
                .extracting(ErrorResponse.FieldErrorDetail::getField)
                .containsExactly("rating");
    }

    private ReviewRequest request(int rating, String comment) {
        return ReviewRequest.builder().customerId(customer.getId()).rating(rating).comment(comment).build();
    }
}
