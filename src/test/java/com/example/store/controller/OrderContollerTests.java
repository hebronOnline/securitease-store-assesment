package com.example.store.controller;

import com.example.store.dto.CreateOrderRequest;
import com.example.store.dto.OrderCustomerDTO;
import com.example.store.dto.OrderDTO;
import com.example.store.exception.GlobalExceptionHandler;
import com.example.store.exception.OrderNotFoundException;
import com.example.store.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    private OrderDTO orderDTO;

    @BeforeEach
    void setUp() {
        OrderCustomerDTO orderCustomerDTO = new OrderCustomerDTO();
        orderCustomerDTO.setId(1L);
        orderCustomerDTO.setName("John Doe");

        orderDTO = new OrderDTO();
        orderDTO.setId(1L);
        orderDTO.setDescription("Test Order");
        orderDTO.setCustomer(orderCustomerDTO);
    }

    @Test
    void testCreateOrder() throws Exception {
        when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(orderDTO);

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("description", "Test Order", "customerId", 1, "productIds", List.of(1, 2)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void testCreateOrderWithNullDescriptionReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("customerId", 1, "productIds", List.of(1)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").value("description should be populated"));
    }

    @Test
    void testCreateOrderWithNoProductsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("description", "Test Order"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.productIds").value("an order must contain at least one product"));
    }

    @Test
    void testGetOrder() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(orderDTO));

        mockMvc.perform(get("/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..description").value("Test Order"))
                .andExpect(jsonPath("$..customer.name").value("John Doe"));
    }

    @Test
    void testGetOrderById() throws Exception {
        when(orderService.getOrder(1L)).thenReturn(orderDTO);

        mockMvc.perform(get("/order/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.description").value("Test Order"))
                .andExpect(jsonPath("$.customer.name").value("John Doe"));
    }

    @Test
    void testGetOrderByIdNotFound() throws Exception {
        when(orderService.getOrder(99L)).thenThrow(new OrderNotFoundException(99L));

        mockMvc.perform(get("/order/99")).andExpect(status().isNotFound());
    }
}
