package com.mk.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class EcommerceApiApplicationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void should_load_context_when_application_starts() {
        // Arrange
        // Act
        // Assert
        assertThat(applicationContext).isNotNull();
    }
}
