package com.example.store.service;

import com.example.store.config.CacheConfig;
import com.example.store.dto.OrderDTO;
import com.example.store.entity.Order;
import com.example.store.exception.OrderNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.OrderRepository;

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
                @CacheEvict(value = CacheConfig.CUSTOMERS_CACHE, allEntries = true)
            })
    public OrderDTO createOrder(Order order) {
        return orderMapper.orderToOrderDTO(orderRepository.save(order));
    }
}
