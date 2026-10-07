package com.mk.ecommerce.service;

import com.mk.ecommerce.dto.CustomerRequest;
import com.mk.ecommerce.dto.CustomerResponse;
import com.mk.ecommerce.entity.Customer;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.DuplicateResourceException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.repository.CustomerRepository;
import com.mk.ecommerce.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void should_return_all_customers_when_customers_exist() {
        // Arrange
        when(customerRepository.findAll()).thenReturn(List.of(customer(1L, "john@example.com")));

        // Act
        List<CustomerResponse> result = customerService.findAll();

        // Assert
        assertThat(result).extracting(CustomerResponse::getEmail).containsExactly("john@example.com");
    }

    @Test
    void should_throw_not_found_when_customer_id_does_not_exist() {
        // Arrange
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void should_register_customer_when_email_is_unique() {
        // Arrange
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        CustomerResponse result = customerService.create(request("john@example.com"));

        // Assert
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void should_throw_duplicate_when_customer_email_already_exists() {
        // Arrange
        when(customerRepository.existsByEmail("john@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.create(request("john@example.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("john@example.com");
    }

    @Test
    void should_update_customer_when_new_email_is_unique() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "john@example.com")));
        when(customerRepository.existsByEmail("new@example.com")).thenReturn(false);

        // Act
        CustomerResponse result = customerService.update(1L, request("new@example.com"));

        // Assert
        assertThat(result.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void should_throw_duplicate_when_updating_to_email_used_by_another_customer() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "john@example.com")));
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.update(1L, request("jane@example.com")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void should_throw_business_rule_when_deleting_customer_with_orders() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(1L, "john@example.com")));
        when(orderRepository.existsByCustomerId(1L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> customerService.delete(1L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void should_delete_customer_when_customer_has_no_orders() {
        // Arrange
        Customer existing = customer(1L, "john@example.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(orderRepository.existsByCustomerId(1L)).thenReturn(false);

        // Act
        customerService.delete(1L);

        // Assert
        verify(customerRepository).delete(existing);
    }

    private Customer customer(Long id, String email) {
        return Customer.builder().id(id).firstName("John").lastName("Doe").email(email).build();
    }

    private CustomerRequest request(String email) {
        return CustomerRequest.builder().firstName("John").lastName("Doe").email(email).build();
    }
}
