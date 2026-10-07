package com.mk.ecommerce;

import com.mk.ecommerce.entity.Category;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.entity.Product;
import com.mk.ecommerce.repository.CategoryRepository;
import com.mk.ecommerce.repository.CustomerRepository;
import com.mk.ecommerce.repository.OrderRepository;
import com.mk.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("integration")
public abstract class IntegrationTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected CategoryRepository categoryRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected CustomerRepository customerRepository;

    @Autowired
    protected OrderRepository orderRepository;

    /** Deletes in foreign-key order; deleting orders cascades to their items and payment. */
    @AfterEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    protected Category saveCategory(String name, String slug) {
        return categoryRepository.save(Category.builder().name(name).slug(slug).build());
    }

    protected Product saveProduct(Category category, String sku, String price, int stock) {
        return productRepository.save(Product.builder()
                .name("Product " + sku)
                .sku(sku)
                .price(new BigDecimal(price))
                .stockQuantity(stock)
                .category(category)
                .build());
    }

    protected Customer saveCustomer(String email) {
        return customerRepository.save(Customer.builder()
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .build());
    }

    protected int stockOf(Long productId) {
        return productRepository.findById(productId).orElseThrow().getStockQuantity();
    }
}
