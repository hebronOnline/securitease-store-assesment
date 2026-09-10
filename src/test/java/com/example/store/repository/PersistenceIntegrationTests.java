package com.example.store.repository;

import com.example.store.entity.Customer;
import com.example.store.entity.Order;
import com.example.store.entity.Product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceIntegrationTests {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Customer persistCustomer(String name) {
        Customer customer = new Customer();
        customer.setName(name);
        return customerRepository.save(customer);
    }

    private Product persistProduct(String description) {
        Product product = new Product();
        product.setDescription(description);
        return productRepository.save(product);
    }

    private Order persistOrder(String description, Customer customer) {
        Order order = new Order();
        order.setDescription(description);
        order.setCustomer(customer);
        return orderRepository.save(order);
    }

    @Test
    @DisplayName("Given a customer, " + "When saved and reloaded, " + "Then it is assigned an id and can be found")
    void testCustomerSaveAndFind() {
        // Given
        Customer saved = persistCustomer("Alice");
        entityManager.flush();
        entityManager.clear();

        // When
        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found.getName()).isEqualTo("Alice");
        assertThat(customerRepository.findAll()).extracting(Customer::getName).contains("Alice");
    }

    @Test
    @DisplayName("Given customers with different names, "
            + "When searching by substring, "
            + "Then only case-insensitive matches are returned")
    void testFindByNameContainingIgnoreCase() {
        // Given
        persistCustomer("Jonathan Smith");
        persistCustomer("Bob Jones");
        persistCustomer("Alice Brown");
        entityManager.flush();
        entityManager.clear();

        // When
        List<Customer> matches = customerRepository.findByNameContainingIgnoreCase("jon");

        // Then
        assertThat(matches).extracting(Customer::getName).containsExactlyInAnyOrder("Jonathan Smith", "Bob Jones");
    }

    @Test
    @DisplayName("Given products, " + "When saved, " + "Then findAll and findByIdIn return them")
    void testProductSaveFindAllAndFindByIdIn() {
        // Given
        Product mouse = persistProduct("Wireless Mouse");
        Product keyboard = persistProduct("Mechanical Keyboard");
        persistProduct("USB-C Hub");
        entityManager.flush();
        entityManager.clear();

        // When
        List<Product> all = productRepository.findAll();
        List<Product> subset = productRepository.findByIdIn(List.of(mouse.getId(), keyboard.getId()));

        // Then
        assertThat(all).hasSize(3);
        assertThat(subset)
                .extracting(Product::getDescription)
                .containsExactlyInAnyOrder("Wireless Mouse", "Mechanical Keyboard");
    }

    @Test
    @DisplayName("Given an order for a customer, "
            + "When saved and reloaded, "
            + "Then the customer association is persisted")
    void testOrderWithCustomerPersists() {
        // Given
        Customer customer = persistCustomer("Carol");
        Order saved = persistOrder("First order", customer);
        entityManager.flush();
        entityManager.clear();

        // When
        Order found = orderRepository.findById(saved.getId()).orElseThrow();

        // Then
        assertThat(found.getDescription()).isEqualTo("First order");
        assertThat(found.getCustomer()).isNotNull();
        assertThat(found.getCustomer().getName()).isEqualTo("Carol");
    }

    @Test
    @DisplayName("Given an order without a customer, "
            + "When flushed, "
            + "Then the not-null customer constraint is violated")
    void testOrderRequiresCustomer() {
        // Given
        Order order = new Order();
        order.setDescription("No customer");

        // When / Then
        assertThatThrownBy(() -> {
                    orderRepository.save(order);
                    entityManager.flush();
                })
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Given products linked to an order on the owning side, "
            + "When reloaded, "
            + "Then the join is persisted and navigable in both directions")
    void testOrderProductManyToMany() {
        // Given
        Customer customer = persistCustomer("Dave");
        Product mouse = persistProduct("Wireless Mouse");
        Product keyboard = persistProduct("Mechanical Keyboard");
        Order order = persistOrder("Bundle", customer);

        // Product owns the join table, so associate from the product side.
        mouse.getOrders().add(order);
        keyboard.getOrders().add(order);
        productRepository.saveAll(List.of(mouse, keyboard));
        entityManager.flush();
        entityManager.clear();

        // When
        Order reloadedOrder = orderRepository.findById(order.getId()).orElseThrow();
        Product reloadedMouse = productRepository.findById(mouse.getId()).orElseThrow();

        // Then - order -> products
        assertThat(reloadedOrder.getProducts())
                .extracting(Product::getDescription)
                .containsExactlyInAnyOrder("Wireless Mouse", "Mechanical Keyboard");

        // Then - product -> orders (reverse navigation)
        assertThat(reloadedMouse.getOrders()).extracting(Order::getId).containsExactly(order.getId());
    }

    @Test
    @DisplayName(
            "Given a product in several orders, " + "When reloaded, " + "Then all containing order IDs are returned")
    void testProductAppearsInMultipleOrders() {
        // Given
        Customer customer = persistCustomer("Erin");
        Product mouse = persistProduct("Wireless Mouse");
        Order first = persistOrder("Order one", customer);
        Order second = persistOrder("Order two", customer);

        mouse.setOrders(new ArrayList<>(List.of(first, second)));
        productRepository.save(mouse);
        entityManager.flush();
        entityManager.clear();

        // When
        Product reloaded = productRepository.findById(mouse.getId()).orElseThrow();

        // Then
        assertThat(reloaded.getOrders())
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }
}
