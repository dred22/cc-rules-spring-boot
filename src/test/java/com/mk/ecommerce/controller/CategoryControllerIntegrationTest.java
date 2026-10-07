package com.mk.ecommerce.controller;

import com.mk.ecommerce.IntegrationTestSupport;
import com.mk.ecommerce.dto.CategoryRequest;
import com.mk.ecommerce.dto.CategoryResponse;
import com.mk.ecommerce.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void should_create_category_when_request_is_valid() {
        // Arrange
        CategoryRequest request = CategoryRequest.builder().name("Books").slug("books").build();

        // Act
        ResponseEntity<CategoryResponse> response =
                restTemplate.postForEntity("/api/v1/categories", request, CategoryResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getName()).isEqualTo("Books");
    }

    @Test
    void should_return_category_when_id_exists() {
        // Arrange
        Category saved = saveCategory("Books", "books");

        // Act
        ResponseEntity<CategoryResponse> response =
                restTemplate.getForEntity("/api/v1/categories/{id}", CategoryResponse.class, saved.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getSlug()).isEqualTo("books");
    }

    @Test
    void should_update_category_when_request_is_valid() {
        // Arrange
        Category saved = saveCategory("Books", "books");
        CategoryRequest request = CategoryRequest.builder().name("Novels").slug("novels").build();

        // Act
        ResponseEntity<CategoryResponse> response = restTemplate.exchange(
                "/api/v1/categories/{id}", HttpMethod.PUT, new HttpEntity<>(request), CategoryResponse.class,
                saved.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getName()).isEqualTo("Novels");
    }

    @Test
    void should_delete_category_when_it_has_no_products() {
        // Arrange
        Category saved = saveCategory("Books", "books");

        // Act
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/categories/{id}", HttpMethod.DELETE, HttpEntity.EMPTY, Void.class, saved.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(categoryRepository.findById(saved.getId())).isEmpty();
    }
}
