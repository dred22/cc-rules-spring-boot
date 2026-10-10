package com.mk.ecommerce.controller;

import com.mk.ecommerce.dto.OrderItemRequest;
import com.mk.ecommerce.dto.OrderItemResponse;
import com.mk.ecommerce.dto.OrderRequest;
import com.mk.ecommerce.dto.OrderResponse;
import com.mk.ecommerce.dto.OrderStatusUpdateRequest;
import com.mk.ecommerce.dto.PaymentResponse;
import com.mk.ecommerce.entity.OrderStatus;
import com.mk.ecommerce.entity.PaymentMethod;
import com.mk.ecommerce.entity.PaymentStatus;
import com.mk.ecommerce.exception.AccessDeniedException;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private OrderService orderService;

    // ---------- GET /api/v1/orders ----------

    @Test
    void should_return_200_with_orders_when_no_filters_are_given() throws Exception {
        // Arrange
        when(orderService.findAll(null, null)).thenReturn(List.of(orderResponse(1L, OrderStatus.PENDING)));

        // Act
        var result = mockMvc.perform(get("/api/v1/orders"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-00000001"));
    }

    @Test
    void should_delegate_customerId_and_status_filters_to_service_when_both_are_given() throws Exception {
        // Arrange
        when(orderService.findAll(1L, OrderStatus.PENDING)).thenReturn(List.of());

        // Act
        mockMvc.perform(get("/api/v1/orders")
                .param("customerId", "1")
                .param("status", "PENDING"));

        // Assert
        verify(orderService).findAll(1L, OrderStatus.PENDING);
    }

    @Test
    void should_return_200_with_empty_array_when_no_orders_match_filters() throws Exception {
        // Arrange
        when(orderService.findAll(1L, OrderStatus.SHIPPED)).thenReturn(List.of());

        // Act
        var result = mockMvc.perform(get("/api/v1/orders")
                .param("customerId", "1")
                .param("status", "SHIPPED"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void should_return_400_when_status_filter_is_not_a_known_order_status() throws Exception {
        // Arrange
        // No stubbing: the request never reaches the service.

        // Act
        var result = mockMvc.perform(get("/api/v1/orders").param("status", "LOST"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'"));
    }

    @Test
    void should_return_400_when_customerId_filter_is_not_a_number() throws Exception {
        // Arrange
        // No stubbing: the request never reaches the service.

        // Act
        var result = mockMvc.perform(get("/api/v1/orders").param("customerId", "abc"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'customerId'"));
    }

    // ---------- GET /api/v1/orders/{id} ----------

    @Test
    void should_return_200_with_order_when_id_exists() throws Exception {
        // Arrange
        when(orderService.findById(1L)).thenReturn(orderResponse(1L, OrderStatus.PENDING));

        // Act
        var result = mockMvc.perform(get("/api/v1/orders/1"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(49.98))
                .andExpect(jsonPath("$.items[0].productName").value("Wireless Mouse"));
    }

    @Test
    void should_return_404_when_order_does_not_exist() throws Exception {
        // Arrange
        when(orderService.findById(99L)).thenThrow(new ResourceNotFoundException("Order not found: 99"));

        // Act
        var result = mockMvc.perform(get("/api/v1/orders/99"));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Order not found: 99"))
                .andExpect(jsonPath("$.path").value("/api/v1/orders/99"));
    }

    @Test
    void should_return_400_when_path_id_is_not_a_number() throws Exception {
        // Arrange
        // No stubbing: the path variable fails conversion before the service is reached.

        // Act
        var result = mockMvc.perform(get("/api/v1/orders/abc"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    // ---------- POST /api/v1/orders ----------

    @Test
    void should_return_201_with_pending_order_when_request_is_valid() throws Exception {
        // Arrange
        OrderRequest request = validOrderRequest();
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(orderResponse(1L, OrderStatus.PENDING));

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.payment.paymentStatus").value("PENDING"));
    }

    @Test
    void should_delegate_deserialized_request_to_service_when_request_is_valid() throws Exception {
        // Arrange
        OrderRequest request = validOrderRequest();
        when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(orderResponse(1L, OrderStatus.PENDING));

        // Act
        mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        verify(orderService).placeOrder(request);
    }

    @Test
    void should_return_400_with_field_errors_when_required_fields_are_missing() throws Exception {
        // Arrange
        String body = "{}";

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(4))
                .andExpect(jsonPath("$.fieldErrors[*].field",
                        containsInAnyOrder("customerId", "shippingAddress", "paymentMethod", "items")));
    }

    @Test
    void should_return_400_with_field_error_when_shipping_address_is_blank() throws Exception {
        // Arrange
        OrderRequest request = validOrderRequest();
        request.setShippingAddress("   ");

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("shippingAddress"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be blank"));
    }

    @Test
    void should_return_400_with_field_error_when_item_quantity_is_below_minimum() throws Exception {
        // Arrange
        OrderRequest request = validOrderRequest();
        request.setItems(List.of(OrderItemRequest.builder().productId(10L).quantity(0).build()));

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be greater than or equal to 1"));
    }

    @Test
    void should_return_400_with_field_error_when_items_list_is_empty() throws Exception {
        // Arrange
        OrderRequest request = validOrderRequest();
        request.setItems(List.of());

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be empty"));
    }

    @Test
    void should_return_400_when_body_is_malformed_json() throws Exception {
        // Arrange
        String malformed = "{\"customerId\": 1, ";

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(malformed));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void should_return_400_when_payment_method_is_not_a_known_value() throws Exception {
        // Arrange
        String body = """
                {
                  "customerId": 1,
                  "shippingAddress": "12 Main St",
                  "paymentMethod": "BITCOIN",
                  "items": [{"productId": 10, "quantity": 2}]
                }
                """;

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void should_return_404_when_customer_does_not_exist() throws Exception {
        // Arrange
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found: 1"));

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(validOrderRequest())));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found: 1"));
    }

    @Test
    void should_return_422_when_product_has_insufficient_stock() throws Exception {
        // Arrange
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new BusinessRuleException("Insufficient stock for product: ELEC-001"));

        // Act
        var result = mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(jsonMapper.writeValueAsString(validOrderRequest())));

        // Assert
        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Insufficient stock for product: ELEC-001"));
    }

    // ---------- PUT /api/v1/orders/{id}/status ----------

    @Test
    void should_return_200_with_updated_order_when_transition_is_allowed() throws Exception {
        // Arrange
        when(orderService.updateStatus(5L, OrderStatus.CONFIRMED))
                .thenReturn(orderResponse(5L, OrderStatus.CONFIRMED));
        String body = jsonMapper.writeValueAsString(statusUpdate(OrderStatus.CONFIRMED));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/5/status")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void should_delegate_path_id_and_target_status_to_service_when_request_is_valid() throws Exception {
        // Arrange
        when(orderService.updateStatus(5L, OrderStatus.CONFIRMED))
                .thenReturn(orderResponse(5L, OrderStatus.CONFIRMED));
        String body = jsonMapper.writeValueAsString(statusUpdate(OrderStatus.CONFIRMED));

        // Act
        mockMvc.perform(put("/api/v1/orders/5/status")
                .contentType("application/json")
                .content(body));

        // Assert
        verify(orderService).updateStatus(5L, OrderStatus.CONFIRMED);
    }

    @Test
    void should_return_400_with_field_error_when_status_is_missing() throws Exception {
        // Arrange
        String body = "{}";

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/5/status")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must not be null"));
    }

    @Test
    void should_return_400_when_status_is_not_a_known_value() throws Exception {
        // Arrange
        String body = "{\"status\": \"LOST\"}";

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/5/status")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void should_return_422_when_status_transition_is_not_allowed() throws Exception {
        // Arrange
        when(orderService.updateStatus(5L, OrderStatus.DELIVERED))
                .thenThrow(new BusinessRuleException("Cannot change order status from PENDING to DELIVERED"));
        String body = jsonMapper.writeValueAsString(statusUpdate(OrderStatus.DELIVERED));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/5/status")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Cannot change order status from PENDING to DELIVERED"));
    }

    @Test
    void should_return_404_when_order_to_update_does_not_exist() throws Exception {
        // Arrange
        when(orderService.updateStatus(99L, OrderStatus.CONFIRMED))
                .thenThrow(new ResourceNotFoundException("Order not found: 99"));
        String body = jsonMapper.writeValueAsString(statusUpdate(OrderStatus.CONFIRMED));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/99/status")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: 99"));
    }

    @Test
    void should_return_400_when_status_update_path_id_is_not_a_number() throws Exception {
        // Arrange
        String body = jsonMapper.writeValueAsString(statusUpdate(OrderStatus.CONFIRMED));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/abc/status")
                .contentType("application/json")
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    // ---------- PUT /api/v1/orders/{id}/cancel ----------

    @Test
    void should_return_200_with_cancelled_order_when_order_is_cancellable() throws Exception {
        // Arrange
        when(orderService.cancelOrder(3L)).thenReturn(orderResponse(3L, OrderStatus.CANCELLED));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/3/cancel"));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void should_delegate_path_id_to_cancelOrder_when_cancel_is_requested() throws Exception {
        // Arrange
        when(orderService.cancelOrder(3L)).thenReturn(orderResponse(3L, OrderStatus.CANCELLED));

        // Act
        mockMvc.perform(put("/api/v1/orders/3/cancel"));

        // Assert
        verify(orderService).cancelOrder(3L);
    }

    @Test
    void should_return_422_when_order_cannot_be_cancelled() throws Exception {
        // Arrange
        when(orderService.cancelOrder(3L))
                .thenThrow(new BusinessRuleException("Cannot change order status from DELIVERED to CANCELLED"));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/3/cancel"));

        // Assert
        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("Cannot change order status from DELIVERED to CANCELLED"));
    }

    @Test
    void should_return_404_when_cancelling_order_that_does_not_exist() throws Exception {
        // Arrange
        when(orderService.cancelOrder(99L)).thenThrow(new ResourceNotFoundException("Order not found: 99"));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/99/cancel"));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: 99"));
    }

    @Test
    void should_return_403_when_service_denies_cancellation() throws Exception {
        // Arrange
        when(orderService.cancelOrder(3L)).thenThrow(new AccessDeniedException("Not allowed to cancel order 3"));

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/3/cancel"));

        // Assert
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Not allowed to cancel order 3"));
    }

    @Test
    void should_return_400_when_cancel_path_id_is_not_a_number() throws Exception {
        // Arrange
        // No stubbing: the path variable fails conversion before the service is reached.

        // Act
        var result = mockMvc.perform(put("/api/v1/orders/abc/cancel"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    // ---------- helpers ----------

    private static OrderRequest validOrderRequest() {
        return OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("12 Main St")
                .notes("Leave at door")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(10L).quantity(2).build()))
                .build();
    }

    private static OrderStatusUpdateRequest statusUpdate(OrderStatus status) {
        return OrderStatusUpdateRequest.builder().status(status).build();
    }

    private static OrderResponse orderResponse(Long id, OrderStatus status) {
        return OrderResponse.builder()
                .id(id)
                .orderNumber("ORD-" + String.format("%08X", id))
                .customerId(1L)
                .status(status)
                .totalAmount(new BigDecimal("49.98"))
                .shippingAddress("12 Main St")
                .notes("Leave at door")
                .items(List.of(OrderItemResponse.builder()
                        .id(100L)
                        .productId(10L)
                        .productName("Wireless Mouse")
                        .quantity(2)
                        .unitPrice(new BigDecimal("24.99"))
                        .subtotal(new BigDecimal("49.98"))
                        .build()))
                .payment(PaymentResponse.builder()
                        .id(200L)
                        .paymentMethod(PaymentMethod.CREDIT_CARD)
                        .paymentStatus(PaymentStatus.PENDING)
                        .amount(new BigDecimal("49.98"))
                        .createdAt(LocalDateTime.of(2026, 10, 10, 12, 0))
                        .build())
                .createdAt(LocalDateTime.of(2026, 10, 10, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 10, 10, 12, 0))
                .build();
    }
}
