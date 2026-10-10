package com.mk.ecommerce.controller;

import com.mk.ecommerce.dto.CustomerRequest;
import com.mk.ecommerce.dto.CustomerResponse;
import com.mk.ecommerce.exception.AccessDeniedException;
import com.mk.ecommerce.exception.BusinessRuleException;
import com.mk.ecommerce.exception.DuplicateResourceException;
import com.mk.ecommerce.exception.ResourceNotFoundException;
import com.mk.ecommerce.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final String BASE_URL = "/api/v1/customers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    void should_return_200_with_customers_when_customers_exist() throws Exception {
        // Arrange
        when(customerService.findAll()).thenReturn(List.of(response(1L, "john.doe@example.com"),
                response(2L, "jane.smith@example.com")));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void should_return_200_with_empty_list_when_no_customers_exist() throws Exception {
        // Arrange
        when(customerService.findAll()).thenReturn(List.of());

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void should_return_200_with_customer_when_id_exists() throws Exception {
        // Arrange
        when(customerService.findById(1L)).thenReturn(response(1L, "john.doe@example.com"));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void should_return_404_with_error_body_when_customer_not_found() throws Exception {
        // Arrange
        when(customerService.findById(99L)).thenThrow(new ResourceNotFoundException("Customer not found: 99"));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", 99L));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Customer not found: 99"))
                .andExpect(jsonPath("$.path").value(BASE_URL + "/99"));
    }

    @Test
    void should_return_403_with_error_body_when_service_denies_access() throws Exception {
        // Arrange
        when(customerService.findById(1L)).thenThrow(new AccessDeniedException("Access denied"));

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void should_return_400_when_path_id_is_not_a_number() throws Exception {
        // Arrange
        // No stubbing: the id cannot be converted to Long before the service is reached.

        // Act
        ResultActions result = mockMvc.perform(get(BASE_URL + "/{id}", "abc"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void should_delegate_findById_to_service_with_id_when_requested() throws Exception {
        // Arrange
        when(customerService.findById(1L)).thenReturn(response(1L, "john.doe@example.com"));

        // Act
        mockMvc.perform(get(BASE_URL + "/{id}", 1L));

        // Assert
        verify(customerService).findById(1L);
    }

    @Test
    void should_return_201_with_created_customer_when_request_is_valid() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        when(customerService.create(request)).thenReturn(response(1L, request.getEmail()));

        // Act
        ResultActions result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void should_delegate_create_to_service_with_request_when_request_is_valid() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        when(customerService.create(request)).thenReturn(response(1L, request.getEmail()));

        // Act
        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        verify(customerService).create(request);
    }

    @Test
    void should_return_400_with_all_field_errors_when_required_fields_missing() throws Exception {
        // Arrange
        String body = "{}";

        // Act
        ResultActions result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("firstName", "lastName", "email")));
    }

    @Test
    void should_return_400_with_email_field_error_when_email_is_malformed() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        request.setEmail("not-an-email");

        // Act
        ResultActions result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("must be a well-formed email address"));
    }

    @Test
    void should_return_400_with_field_error_when_first_name_exceeds_max_length() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        request.setFirstName("a".repeat(101));

        // Act
        ResultActions result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("firstName"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("size must be between 0 and 100"));
    }

    @Test
    void should_return_400_with_field_error_when_phone_exceeds_max_length() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        request.setPhone("1".repeat(21));

        // Act
        ResultActions result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("phone"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("size must be between 0 and 20"));
    }

    @Test
    void should_return_400_with_error_body_when_create_json_is_malformed() throws Exception {
        // Arrange
        String malformed = "{\"firstName\": \"John\", ";

        // Act
        ResultActions result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformed));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));
    }

    @Test
    void should_return_409_with_error_body_when_email_already_exists() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        when(customerService.create(request))
                .thenThrow(new DuplicateResourceException("Customer email already exists: john.doe@example.com"));

        // Act
        ResultActions result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Customer email already exists: john.doe@example.com"));
    }

    @Test
    void should_return_200_with_updated_customer_when_request_is_valid() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        request.setFirstName("Johnny");
        when(customerService.update(1L, request)).thenReturn(CustomerResponse.builder()
                .id(1L)
                .firstName("Johnny")
                .lastName("Doe")
                .email("john.doe@example.com")
                .build());

        // Act
        ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstName").value("Johnny"));
    }

    @Test
    void should_delegate_update_to_service_with_id_and_request_when_request_is_valid() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        when(customerService.update(1L, request)).thenReturn(response(1L, request.getEmail()));

        // Act
        mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        verify(customerService).update(1L, request);
    }

    @Test
    void should_return_400_with_field_errors_when_update_body_is_invalid() throws Exception {
        // Arrange
        String body = "{\"firstName\": \"\", \"lastName\": \"Doe\", \"email\": \"bad\"}";

        // Act
        ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[*].field", containsInAnyOrder("firstName", "email")));
    }

    @Test
    void should_return_400_when_update_path_id_is_not_a_number() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();

        // Act
        ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", "abc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void should_return_404_with_error_body_when_updating_missing_customer() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        when(customerService.update(99L, request)).thenThrow(new ResourceNotFoundException("Customer not found: 99"));

        // Act
        ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", 99L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found: 99"));
    }

    @Test
    void should_return_409_with_error_body_when_updated_email_belongs_to_another_customer() throws Exception {
        // Arrange
        CustomerRequest request = validRequest();
        when(customerService.update(1L, request))
                .thenThrow(new DuplicateResourceException("Customer email already exists: john.doe@example.com"));

        // Act
        ResultActions result = mockMvc.perform(put(BASE_URL + "/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request)));

        // Assert
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void should_return_204_with_empty_body_when_customer_deleted() throws Exception {
        // Arrange
        // Service delete is void and succeeds by default.

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void should_delegate_delete_to_service_with_id_when_customer_deleted() throws Exception {
        // Arrange
        // Service delete is void and succeeds by default.

        // Act
        mockMvc.perform(delete(BASE_URL + "/{id}", 1L));

        // Assert
        verify(customerService).delete(1L);
    }

    @Test
    void should_return_404_with_error_body_when_deleting_missing_customer() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Customer not found: 99")).when(customerService).delete(99L);

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", 99L));

        // Assert
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Customer not found: 99"));
    }

    @Test
    void should_return_422_with_error_body_when_customer_has_existing_orders() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Cannot delete customer with existing orders: 1"))
                .when(customerService).delete(anyLong());

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", 1L));

        // Assert
        result.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Cannot delete customer with existing orders: 1"));
    }

    @Test
    void should_return_400_when_delete_path_id_is_not_a_number() throws Exception {
        // Arrange
        // No stubbing: the id cannot be converted to Long before the service is reached.

        // Act
        ResultActions result = mockMvc.perform(delete(BASE_URL + "/{id}", "abc"));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    private CustomerRequest validRequest() {
        return CustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1-555-0100")
                .address("1 Main Street")
                .build();
    }

    private CustomerResponse response(Long id, String email) {
        return CustomerResponse.builder()
                .id(id)
                .firstName("John")
                .lastName("Doe")
                .email(email)
                .phone("+1-555-0100")
                .address("1 Main Street")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }
}
