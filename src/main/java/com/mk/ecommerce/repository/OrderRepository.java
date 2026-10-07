package com.mk.ecommerce.repository;

import com.mk.ecommerce.entity.Order;
import com.mk.ecommerce.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByCustomerId(Long customerId);

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
