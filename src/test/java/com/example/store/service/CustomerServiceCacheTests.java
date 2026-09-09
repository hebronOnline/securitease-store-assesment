package com.example.store.service;

import com.example.store.config.CacheConfig;
import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class CustomerServiceCacheTests {

    @TestConfiguration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(CacheConfig.ORDERS_CACHE, CacheConfig.CUSTOMERS_CACHE);
        }

        @Bean
        CustomerService customerService(CustomerRepository repository, CustomerMapper mapper) {
            return new CustomerService(repository, mapper);
        }
    }

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private CustomerMapper customerMapper;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        reset(customerRepository, customerMapper);
    }

    @Test
    @DisplayName("Given getAllCustomers called twice, "
            + "When the second call runs, "
            + "Then the repository is queried only once (cache hit)")
    void testGetAllCustomers_isCached() {
        when(customerRepository.findAll()).thenReturn(List.of(new Customer()));
        when(customerMapper.customersToCustomerDTOs(anyList())).thenReturn(List.of(new CustomerDTO()));

        customerService.getAllCustomers();
        customerService.getAllCustomers();

        verify(customerRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Given a name search repeated with different casing/whitespace, "
            + "When findCustomersByName runs again, "
            + "Then the repository is queried only once (normalised cache key hit)")
    void testFindCustomersByName_isCachedByNormalisedKey() {
        when(customerRepository.findByNameContainingIgnoreCase("john")).thenReturn(List.of(new Customer()));
        when(customerMapper.customersToCustomerDTOs(anyList())).thenReturn(List.of(new CustomerDTO()));

        customerService.findCustomersByName("john");
        customerService.findCustomersByName("  JOHN  ");

        verify(customerRepository, times(1)).findByNameContainingIgnoreCase("john");
    }

    @Test
    @DisplayName("Given a search that returns no matches, "
            + "When findCustomersByName is called twice, "
            + "Then the empty result is not cached and the repository is queried each time")
    void testFindCustomersByName_emptyResultNotCached() {
        when(customerRepository.findByNameContainingIgnoreCase("zzz")).thenReturn(List.of());
        when(customerMapper.customersToCustomerDTOs(anyList())).thenReturn(List.of());

        customerService.findCustomersByName("zzz");
        customerService.findCustomersByName("zzz");

        verify(customerRepository, times(2)).findByNameContainingIgnoreCase("zzz");
    }

    @Test
    @DisplayName("Given cached customer reads, "
            + "When createCustomer is called, "
            + "Then the customer cache is evicted and the next reads hit the repository again")
    void testCreateCustomer_evictsCustomerCache() {
        Customer customer = new Customer();
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(customerRepository.findByNameContainingIgnoreCase("john")).thenReturn(List.of(customer));
        when(customerMapper.customersToCustomerDTOs(anyList())).thenReturn(List.of(new CustomerDTO()));
        when(customerMapper.customerToCustomerDTO(customer)).thenReturn(new CustomerDTO());
        when(customerRepository.save(customer)).thenReturn(customer);

        customerService.getAllCustomers();
        customerService.findCustomersByName("john");

        customerService.createCustomer(customer);

        customerService.getAllCustomers();
        customerService.findCustomersByName("john");

        verify(customerRepository, times(2)).findAll();
        verify(customerRepository, times(2)).findByNameContainingIgnoreCase("john");
    }
}
