package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.OrderItemRequest;
import com.mk.ecommerce.dto.OrderRequest;
import com.mk.ecommerce.dto.OrderResponse;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.entity.Order;
import com.mk.ecommerce.entity.OrderItem;
import com.mk.ecommerce.entity.OrderStatus;
import com.mk.ecommerce.entity.Payment;
import com.mk.ecommerce.entity.PaymentMethod;
import com.mk.ecommerce.entity.PaymentStatus;
import com.mk.ecommerce.entity.Product;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CustomerRepository;
import com.mk.ecommerce.repository.OrderRepository;
import com.mk.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    // ---------- placeOrder ----------

    @Test
    void should_place_pending_order_with_calculated_total_when_stock_is_available() {
        // Arrange
        Product product = product(10L, "24.99", 10, true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        OrderResponse result = orderService.placeOrder(orderRequest(10L, 2));

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("49.98");
        assertThat(result.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getPayment().getAmount()).isEqualByComparingTo("49.98");
    }

    @Test
    void should_reduce_stock_when_order_is_placed() {
        // Arrange
        Product product = product(10L, "24.99", 10, true);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        orderService.placeOrder(orderRequest(10L, 3));

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void should_throw_not_found_when_customer_does_not_exist() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(10L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    void should_throw_business_rule_when_stock_is_insufficient() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product(10L, "24.99", 1, true)));

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(10L, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void should_throw_business_rule_when_product_is_inactive() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product(10L, "24.99", 10, false)));

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(10L, 1)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not available");
    }

    @Test
    void should_throw_not_found_when_product_does_not_exist() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.placeOrder(orderRequest(10L, 1)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    // ---------- cancelOrder ----------

    @Test
    void should_cancel_order_when_order_is_pending() {
        // Arrange
        Product product = product(10L, "24.99", 8, true);
        Order order = order(OrderStatus.PENDING, product, 2);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product));

        // Act
        OrderResponse result = orderService.cancelOrder(5L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void should_restore_stock_when_order_is_cancelled() {
        // Arrange
        Product product = product(10L, "24.99", 8, true);
        Order order = order(OrderStatus.CONFIRMED, product, 2);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product));

        // Act
        orderService.cancelOrder(5L);

        // Assert
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void should_mark_payment_refunded_when_cancelled_order_payment_was_completed() {
        // Arrange
        Product product = product(10L, "24.99", 8, true);
        Order order = order(OrderStatus.CONFIRMED, product, 2);
        order.assignPayment(Payment.builder()
                .paymentMethod(PaymentMethod.UPI)
                .paymentStatus(PaymentStatus.COMPLETED)
                .amount(new BigDecimal("49.98"))
                .build());
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product));

        // Act
        OrderResponse result = orderService.cancelOrder(5L);

        // Assert
        assertThat(result.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void should_keep_payment_status_when_cancelled_order_payment_was_not_completed() {
        // Arrange
        Product product = product(10L, "24.99", 8, true);
        Order order = order(OrderStatus.PENDING, product, 2);
        order.assignPayment(Payment.builder()
                .paymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .paymentStatus(PaymentStatus.PENDING)
                .amount(new BigDecimal("49.98"))
                .build());
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));
        when(productRepository.findWithLockById(10L)).thenReturn(Optional.of(product));

        // Act
        OrderResponse result = orderService.cancelOrder(5L);

        // Assert
        assertThat(result.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void should_throw_business_rule_when_cancelling_delivered_order() {
        // Arrange
        Order order = order(OrderStatus.DELIVERED, product(10L, "24.99", 8, true), 2);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DELIVERED")
                .hasMessageContaining("CANCELLED");
    }

    @Test
    void should_throw_business_rule_when_cancelling_already_cancelled_order() {
        // Arrange
        Order order = order(OrderStatus.CANCELLED, product(10L, "24.99", 8, true), 2);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED to CANCELLED");
    }

    @Test
    void should_throw_not_found_when_cancelling_missing_order() {
        // Arrange
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.cancelOrder(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("5");
    }

    // ---------- updateStatus ----------

    @Test
    void should_update_status_when_transition_is_allowed() {
        // Arrange
        Order order = order(OrderStatus.PENDING, product(10L, "24.99", 8, true), 2);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        // Act
        OrderResponse result = orderService.updateStatus(5L, OrderStatus.CONFIRMED);

        // Assert
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void should_throw_business_rule_when_transition_skips_a_step() {
        // Arrange
        Order order = order(OrderStatus.PENDING, product(10L, "24.99", 8, true), 2);
        when(orderRepository.findById(5L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThatThrownBy(() -> orderService.updateStatus(5L, OrderStatus.SHIPPED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING to SHIPPED");
    }

    // ---------- findById ----------

    @Test
    void should_throw_not_found_when_order_id_does_not_exist() {
        // Arrange
        when(orderRepository.findById(5L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.findById(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- helpers ----------

    private Customer customer() {
        return Customer.builder().id(1L).firstName("John").lastName("Doe").email("john@example.com").build();
    }

    private Product product(Long id, String price, int stock, boolean active) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .sku("SKU-" + id)
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .active(active)
                .build();
    }

    private Order order(OrderStatus status, Product product, int quantity) {
        Order order = Order.builder()
                .id(5L)
                .customer(customer())
                .status(status)
                .shippingAddress("123 Maple Street")
                .build();
        order.addItem(OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build());
        return order;
    }

    private OrderRequest orderRequest(Long productId, int quantity) {
        return OrderRequest.builder()
                .customerId(1L)
                .shippingAddress("123 Maple Street")
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .items(List.of(OrderItemRequest.builder().productId(productId).quantity(quantity).build()))
                .build();
    }
}
