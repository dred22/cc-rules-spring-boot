package com.mk.ecommerce.controller;

import com.mk.ecommerce.IntegrationTestSupport;
import com.mk.ecommerce.dto.OrderItemRequest;
import com.mk.ecommerce.dto.OrderRequest;
import com.mk.ecommerce.dto.OrderResponse;
import com.mk.ecommerce.dto.OrderStatusUpdateRequest;
import com.mk.ecommerce.entity.Category;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.entity.OrderStatus;
import com.mk.ecommerce.entity.PaymentMethod;
import com.mk.ecommerce.entity.PaymentStatus;
import com.mk.ecommerce.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderControllerIntegrationTest extends IntegrationTestSupport {

    private Customer customer;
    private Product product;

    @BeforeEach
    void setUpData() {
        Category category = saveCategory("Electronics", "electronics");
        product = saveProduct(category, "ELEC-001", "24.99", 10);
        customer = saveCustomer("john.doe@example.com");
    }

    @Test
    void should_place_order_when_stock_is_available() {
        // Arrange
        OrderRequest request = orderRequest(2);

        // Act
        ResponseEntity<OrderResponse> response =
                restTemplate.postForEntity("/api/v1/orders", request, OrderResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        OrderResponse body = response.getBody();
        assertThat(body.getOrderNumber()).matches("ORD-[0-9A-F]{8}");
        assertThat(body.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(body.getTotalAmount()).isEqualByComparingTo("49.98");
        assertThat(body.getItems()).hasSize(1);
        assertThat(body.getItems().getFirst().getSubtotal()).isEqualByComparingTo("49.98");
        assertThat(body.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void should_reduce_stock_when_order_is_placed() {
        // Arrange
        OrderRequest request = orderRequest(3);

        // Act
        restTemplate.postForEntity("/api/v1/orders", request, OrderResponse.class);

        // Assert
        assertThat(stockOf(product.getId())).isEqualTo(7);
    }

    @Test
    void should_return_order_when_id_exists() {
        // Arrange
        Long orderId = placeOrder(1).getId();

        // Act
        ResponseEntity<OrderResponse> response =
                restTemplate.getForEntity("/api/v1/orders/{id}", OrderResponse.class, orderId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getCustomerId()).isEqualTo(customer.getId());
    }

    @Test
    void should_confirm_order_when_status_updated_from_pending_to_confirmed() {
        // Arrange
        Long orderId = placeOrder(1).getId();
        OrderStatusUpdateRequest request = OrderStatusUpdateRequest.builder().status(OrderStatus.CONFIRMED).build();

        // Act
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/api/v1/orders/{id}/status", HttpMethod.PUT, new HttpEntity<>(request), OrderResponse.class,
                orderId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void should_cancel_order_and_restore_stock_when_order_is_pending() {
        // Arrange
        Long orderId = placeOrder(4).getId();

        // Act
        ResponseEntity<OrderResponse> response = restTemplate.exchange(
                "/api/v1/orders/{id}/cancel", HttpMethod.PUT, HttpEntity.EMPTY, OrderResponse.class, orderId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockOf(product.getId())).isEqualTo(10);
    }

    private OrderResponse placeOrder(int quantity) {
        return restTemplate.postForEntity("/api/v1/orders", orderRequest(quantity), OrderResponse.class).getBody();
    }

    private OrderRequest orderRequest(int quantity) {
        return OrderRequest.builder()
                .customerId(customer.getId())
                .shippingAddress("123 Maple Street, Springfield")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder()
                        .productId(product.getId())
                        .quantity(quantity)
                        .build()))
                .build();
    }
}
