package com.mk.ecommerce.repository;

import com.mk.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsByCategoryId(Long categoryId);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockById(Long id);
}
