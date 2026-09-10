package com.example.store.service;

import com.example.store.config.CacheConfig;
import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.OrderNotFoundException;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final OrderMapper orderMapper;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.ORDERS_CACHE, key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<OrderDTO> getAllOrders() {
        return orderMapper.ordersToOrderDTOs(orderRepository.findAll());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.ORDERS_CACHE, key = "#id")
    public OrderDTO getOrder(Long id) {
        return orderRepository
                .findById(id)
                .map(orderMapper::orderToOrderDTO)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    @Caching(
            evict = {
                @CacheEvict(value = CacheConfig.ORDERS_CACHE, key = "'all'"),
                @CacheEvict(value = CacheConfig.CUSTOMERS_CACHE, allEntries = true),
                @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true)
            })
    public OrderDTO createOrder(CreateOrderRequest request) {
        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.getCustomerId()));
        List<Product> products = resolveProducts(request.getProductIds());

        Order order = orderMapper.createOrderRequestToOrder(request);
        order.setCustomer(customer);
        Order savedOrder = orderRepository.save(order);

        products.forEach(product -> product.getOrders().add(savedOrder));
        productRepository.saveAll(products);

        savedOrder.setProducts(products);
        return orderMapper.orderToOrderDTO(savedOrder);
    }

    private List<Product> resolveProducts(List<Long> productIds) {
        List<Product> products = productRepository.findByIdIn(productIds);
        if (products.size() != productIds.size()) {
            Long missingId = productIds.stream()
                    .filter(id -> products.stream()
                            .noneMatch(product -> product.getId().equals(id)))
                    .findFirst()
                    .orElseThrow();
            throw new ProductNotFoundException(missingId);
        }
        return products;
    }
}
