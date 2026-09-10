-- Create product table
CREATE TABLE product (
                         id BIGSERIAL PRIMARY KEY,
                         description VARCHAR(255) NOT NULL
);

-- Join table for the many-to-many relationship between order and product.
-- A single order contains 1 or more products; a product can appear in many orders.
CREATE TABLE order_product (
                         order_id BIGINT NOT NULL,
                         product_id BIGINT NOT NULL,
                         PRIMARY KEY (order_id, product_id),
                         CONSTRAINT fk_order_product_order FOREIGN KEY (order_id) REFERENCES "order" (id),
                         CONSTRAINT fk_order_product_product FOREIGN KEY (product_id) REFERENCES product (id)
);

-- Support reverse lookups (products -> containing order IDs) without scanning the join table.
CREATE INDEX idx_order_product_product_id ON order_product (product_id);
