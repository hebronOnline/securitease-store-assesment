package com.example.store.service;

import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTests {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("Given a name with surrounding whitespace, "
            + "When findCustomersByName is called, "
            + "Then the repository is queried with the trimmed name and mapped results are returned")
    void testGivenNameWithWhitespace_whenFindCustomersByName_thenQueriesRepositoryWithTrimmedName() {
        // Given
        CustomerDTO dto = new CustomerDTO();
        dto.setName("John Doe");
        when(customerRepository.findByNameContainingIgnoreCase("john")).thenReturn(List.of(new Customer()));
        when(customerMapper.customersToCustomerDTOs(anyList())).thenReturn(List.of(dto));

        // When
        List<CustomerDTO> result = customerService.findCustomersByName("  john  ");

        // Then
        assertThat(result).extracting(CustomerDTO::getName).containsExactly("John Doe");
        verify(customerRepository).findByNameContainingIgnoreCase("john");
        verify(customerRepository, never()).findAll();
    }

    @Test
    @DisplayName("Given a blank name, "
            + "When findCustomersByName is called, "
            + "Then all customers are returned without a name query")
    void testGivenBlankName_whenFindCustomersByName_thenReturnsAllCustomers() {
        // Given
        when(customerRepository.findAll()).thenReturn(List.of(new Customer()));
        when(customerMapper.customersToCustomerDTOs(anyList())).thenReturn(List.of(new CustomerDTO()));

        // When
        customerService.findCustomersByName("   ");

        // Then
        verify(customerRepository).findAll();
        verify(customerRepository, never()).findByNameContainingIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Given a null name, "
            + "When findCustomersByName is called, "
            + "Then all customers are returned without a name query")
    void testGivenNullName_whenFindCustomersByName_thenReturnsAllCustomers() {
        // Given
        when(customerRepository.findAll()).thenReturn(List.of(new Customer()));
        when(customerMapper.customersToCustomerDTOs(anyList())).thenReturn(List.of(new CustomerDTO()));

        // When
        customerService.findCustomersByName(null);

        // Then
        verify(customerRepository).findAll();
        verify(customerRepository, never()).findByNameContainingIgnoreCase(anyString());
    }
}
