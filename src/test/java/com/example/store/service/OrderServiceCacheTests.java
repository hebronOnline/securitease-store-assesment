package com.example.store.service;

import com.example.store.config.CacheConfig;
import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class OrderServiceCacheTests {

    @TestConfiguration
    @EnableCaching
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(
                    CacheConfig.ORDERS_CACHE, CacheConfig.CUSTOMERS_CACHE, CacheConfig.PRODUCTS_CACHE);
        }

        @Bean
        OrderService orderService(
                OrderRepository orderRepository,
                ProductRepository productRepository,
                CustomerRepository customerRepository,
                OrderMapper mapper) {
            return new OrderService(orderRepository, productRepository, customerRepository, mapper);
        }
    }

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private CustomerRepository customerRepository;

    @MockitoBean
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        reset(orderRepository, productRepository, customerRepository, orderMapper);
    }

    @Test
    @DisplayName("Given getOrder called twice for the same id, "
            + "When the second call runs, "
            + "Then the repository is queried only once (cache hit)")
    void testGetOrderById_isCachedById() {
        Order order = new Order();
        order.setId(1L);
        OrderDTO dto = new OrderDTO();
        dto.setId(1L);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.orderToOrderDTO(order)).thenReturn(dto);

        OrderDTO first = orderService.getOrder(1L);
        OrderDTO second = orderService.getOrder(1L);

        assertThat(first).isSameAs(second);
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Given getAllOrders called twice, "
            + "When the second call runs, "
            + "Then the repository is queried only once (cache hit)")
    void testGetAllOrders_isCached() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order()));
        when(orderMapper.ordersToOrderDTOs(anyList())).thenReturn(List.of(new OrderDTO()));

        orderService.getAllOrders();
        orderService.getAllOrders();

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Given a cached order list and by-id entry, "
            + "When createOrder is called, "
            + "Then the list cache is evicted but the by-id entry survives")
    void testCreateOrder_evictsListButKeepsById() {
        // Given
        Order order = new Order();
        order.setId(1L);
        OrderDTO dto = new OrderDTO();
        dto.setId(1L);

        Product product = new Product();
        product.setId(100L);
        product.setOrders(new ArrayList<>());

        Customer customer = new Customer();
        customer.setId(5L);
        customer.setName("Muriel Donnelly");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setDescription("Test Order");
        request.setCustomerId(5L);
        request.setProductIds(List.of(100L));

        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.orderToOrderDTO(order)).thenReturn(dto);
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderMapper.ordersToOrderDTOs(anyList())).thenReturn(List.of(dto));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.createOrderRequestToOrder(request)).thenReturn(order);
        when(productRepository.findByIdIn(List.of(100L))).thenReturn(new ArrayList<>(List.of(product)));

        orderService.getOrder(1L);
        orderService.getAllOrders();

        // When
        orderService.createOrder(request);

        // Then
        orderService.getOrder(1L);
        orderService.getAllOrders();

        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(2)).findAll();
    }
}
