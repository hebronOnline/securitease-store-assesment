package com.example.store.service;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderDTO;
import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;
import com.example.store.exception.CustomerNotFoundException;
import com.example.store.exception.ProductNotFoundException;
import com.example.store.mapper.OrderMapper;
import com.example.store.repository.CustomerRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ProductRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Given a request with valid product IDs, "
            + "When createOrder is called, "
            + "Then the order is saved and each product is linked on the owning side")
    void testGivenValidProducts_whenCreateOrder_thenLinksProductsAndSaves() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setDescription("Test Order");
        request.setCustomerId(5L);
        request.setProductIds(List.of(10L, 20L));

        Customer customer = new Customer();
        customer.setId(5L);
        customer.setName("Muriel Donnelly");

        Product first = new Product();
        first.setId(10L);
        first.setOrders(new ArrayList<>());
        Product second = new Product();
        second.setId(20L);
        second.setOrders(new ArrayList<>());

        Order order = new Order();
        order.setId(1L);
        OrderDTO dto = new OrderDTO();
        dto.setId(1L);

        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdIn(List.of(10L, 20L))).thenReturn(new ArrayList<>(List.of(first, second)));
        when(orderMapper.createOrderRequestToOrder(request)).thenReturn(order);
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.orderToOrderDTO(order)).thenReturn(dto);

        // When
        OrderDTO result = orderService.createOrder(request);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getCustomer().getName()).isEqualTo("Muriel Donnelly");
        assertThat(first.getOrders()).containsExactly(order);
        assertThat(second.getOrders()).containsExactly(order);
        verify(orderRepository).save(order);
        verify(productRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Given a request referencing a product that does not exist, "
            + "When createOrder is called, "
            + "Then a ProductNotFoundException is thrown and no order is saved")
    void testGivenMissingProduct_whenCreateOrder_thenThrowsNotFound() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setDescription("Test Order");
        request.setCustomerId(5L);
        request.setProductIds(List.of(10L, 99L));

        Customer customer = new Customer();
        customer.setId(5L);
        customer.setName("Muriel Donnelly");

        Product existing = new Product();
        existing.setId(10L);
        existing.setOrders(new ArrayList<>());

        when(customerRepository.findById(5L)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdIn(List.of(10L, 99L))).thenReturn(new ArrayList<>(List.of(existing)));

        // When / Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Given a request referencing a customer that does not exist, "
            + "When createOrder is called, "
            + "Then a CustomerNotFoundException is thrown and no order is saved")
    void testGivenMissingCustomer_whenCreateOrder_thenThrowsNotFound() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest();
        request.setDescription("Test Order");
        request.setCustomerId(999L);
        request.setProductIds(List.of(10L));

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("999");
        verify(orderRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
