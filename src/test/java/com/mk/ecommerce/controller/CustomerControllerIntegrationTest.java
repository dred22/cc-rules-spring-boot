package com.mk.ecommerce.controller;

import com.mk.ecommerce.IntegrationTestSupport;
import com.mk.ecommerce.dto.CustomerRequest;
import com.mk.ecommerce.dto.CustomerResponse;
import com.mk.ecommerce.entity.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerControllerIntegrationTest extends IntegrationTestSupport {

    @Test
    void should_register_customer_when_request_is_valid() {
        // Arrange
        CustomerRequest request = CustomerRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .build();

        // Act
        ResponseEntity<CustomerResponse> response =
                restTemplate.postForEntity("/api/v1/customers", request, CustomerResponse.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getEmail()).isEqualTo("jane.smith@example.com");
    }

    @Test
    void should_return_customer_when_id_exists() {
        // Arrange
        Customer saved = saveCustomer("john.doe@example.com");

        // Act
        ResponseEntity<CustomerResponse> response =
                restTemplate.getForEntity("/api/v1/customers/{id}", CustomerResponse.class, saved.getId());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getFirstName()).isEqualTo("John");
    }
}
