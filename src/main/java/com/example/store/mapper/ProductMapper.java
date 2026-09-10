package com.example.store.mapper;

import com.example.store.dto.CreateProductRequest;
import com.example.store.dto.ProductDTO;
import com.example.store.entity.Order;
import com.example.store.entity.Product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDTO productToProductDTO(Product product);

    List<ProductDTO> productsToProductDTOs(List<Product> products);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orders", ignore = true)
    Product createProductRequestToProduct(CreateProductRequest request);

    default List<Long> ordersToOrderIds(List<Order> orders) {
        if (orders == null) {
            return null;
        }
        return orders.stream().map(Order::getId).collect(Collectors.toList());
    }
}
