package com.mk.ecommerce.controller;

import com.mk.ecommerce.IntegrationTestSupport;
import com.mk.ecommerce.dto.ProductRequest;
import com.mk.ecommerce.dto.ProductResponse;
import com.mk.ecommerce.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void should_create_product_when_request_is_valid() {
        // Arrange
        Category category = saveCategory("Books", "books");
        ProductRequest request = ProductRequest.builder()
                .name("Clean Code")
                .price(new BigDecimal("32.50"))
                .sku("BOOK-001")
                .stockQuantity(20)
                .categoryId(category.getId())
                .build();

        // Act
        ResponseEntity<ProductResponse> response =
                restTemplate.postForEntity("/api/v1/products", request, ProductResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getSku()).isEqualTo("BOOK-001");
        assertThat(response.getBody().getActive()).isTrue();
        assertThat(response.getBody().getCategoryName()).isEqualTo("Books");
    }

    @Test
    void should_return_product_when_id_exists() {
        // Arrange
        Category category = saveCategory("Books", "books");
        Long id = saveProduct(category, "BOOK-001", "32.50", 20).getId();

        // Act
        ResponseEntity<ProductResponse> response =
                restTemplate.getForEntity("/api/v1/products/{id}", ProductResponse.class, id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getPrice()).isEqualByComparingTo("32.50");
    }

    @Test
    void should_list_only_products_of_category_when_category_filter_given() {
        // Arrange
        Category books = saveCategory("Books", "books");
        Category clothing = saveCategory("Clothing", "clothing");
        saveProduct(books, "BOOK-001", "32.50", 20);
        saveProduct(clothing, "CLTH-001", "15.00", 30);

        // Act
        ResponseEntity<ProductResponse[]> response = restTemplate.getForEntity(
                "/api/v1/products?categoryId={id}", ProductResponse[].class, books.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(ProductResponse::getSku).containsExactly("BOOK-001");
    }
}
