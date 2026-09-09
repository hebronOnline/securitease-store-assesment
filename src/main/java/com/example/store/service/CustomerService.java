package com.example.store.service;

import com.example.store.config.CacheConfig;
import com.example.store.dto.CustomerDTO;
import com.example.store.entity.Customer;
import com.example.store.mapper.CustomerMapper;
import com.example.store.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CUSTOMERS_CACHE, key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<CustomerDTO> getAllCustomers() {
        return customerMapper.customersToCustomerDTOs(customerRepository.findAll());
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheConfig.CUSTOMERS_CACHE,
            key = "'search:' + #name.trim().toLowerCase()",
            condition = "#name != null && !#name.trim().isEmpty()",
            unless = "#result == null || #result.isEmpty()")
    public List<CustomerDTO> findCustomersByName(String name) {
        String trimmed = name == null ? null : name.trim();
        if (ObjectUtils.isEmpty(trimmed)) {
            return getAllCustomers();
        }
        return customerMapper.customersToCustomerDTOs(customerRepository.findByNameContainingIgnoreCase(trimmed));
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CUSTOMERS_CACHE, allEntries = true)
    public CustomerDTO createCustomer(Customer customer) {
        return customerMapper.customerToCustomerDTO(customerRepository.save(customer));
    }
}
