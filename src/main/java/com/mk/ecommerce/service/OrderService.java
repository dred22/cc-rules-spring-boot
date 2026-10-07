package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.OrderItemRequest;
import com.mk.ecommerce.dto.OrderRequest;
import com.mk.ecommerce.dto.OrderResponse;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.entity.Order;
import com.mk.ecommerce.entity.OrderItem;
import com.mk.ecommerce.entity.OrderStatus;
import com.mk.ecommerce.entity.Payment;
import com.mk.ecommerce.entity.PaymentStatus;
import com.mk.ecommerce.entity.Product;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CustomerRepository;
import com.mk.ecommerce.repository.OrderRepository;
import com.mk.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private static final int LOW_STOCK_THRESHOLD = 10;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(Long customerId, OrderStatus status) {
        List<Order> orders;
        if (customerId != null) {
            orders = orderRepository.findByCustomerId(customerId, Pageable.unpaged()).getContent();
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, Pageable.unpaged()).getContent();
        } else {
            orders = orderRepository.findAll();
        }
        return orders.stream()
                .filter(o -> status == null || o.getStatus() == status)
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return OrderResponse.from(getOrder(id));
    }

    /**
     * Places an order: stock is reduced now, not at shipment.
     */
    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.getCustomerId()));

        Order order = Order.builder()
                .customer(customer)
                .shippingAddress(request.getShippingAddress())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        Map<Long, Product> reducedProducts = new LinkedHashMap<>();
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = lockProduct(itemRequest.getProductId());
            if (!product.getActive()) {
                throw new BusinessRuleException("Product is not available: " + product.getSku());
            }
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new BusinessRuleException("Insufficient stock for product: " + product.getSku());
            }
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            reducedProducts.put(product.getId(), product);
            order.addItem(OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .build());
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }

        warnOnLowStock(reducedProducts.values());

        order.setTotalAmount(total);
        order.assignPayment(Payment.builder()
                .paymentMethod(request.getPaymentMethod())
                .amount(total)
                .build());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus target) {
        Order order = getOrder(orderId);

        if (!order.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Cannot change order status from " + order.getStatus() + " to " + target);
        }

        if (target == OrderStatus.CANCELLED) {
            restoreStock(order);
            if (order.getPayment() != null && order.getPayment().getPaymentStatus() == PaymentStatus.COMPLETED) {
                order.getPayment().setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }
        order.setStatus(target);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        return updateStatus(orderId, OrderStatus.CANCELLED);
    }

    private void warnOnLowStock(Iterable<Product> products) {
        for (Product product : products) {
            if (product.getStockQuantity() < LOW_STOCK_THRESHOLD) {
                log.warn("Low stock alert: {} has {} units left", product.getName(), product.getStockQuantity());
            }
        }
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product product = lockProduct(item.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
    }

    private Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private Product lockProduct(Long productId) {
        return productRepository.findWithLockById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
    }
}
