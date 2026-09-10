-- Advance the identity sequences past any explicitly-inserted seed ids.
--
-- The seed data (data.sql) inserts customer and order rows with EXPLICIT id values.
-- PostgreSQL does NOT bump a BIGSERIAL's underlying sequence when ids are supplied
-- explicitly, so the sequence still starts at 1. The first application insert would
-- then generate ids 1, 2, 3, ... which collide with the seeded rows and fail with
-- "duplicate key value violates unique constraint".
--
-- This migration runs last (changelog-4), after the seed load (2-data) and the product
-- schema (3-product-schema), so all three tables exist.
--
-- setval(seq, value, is_called):
--   * When the table has rows, use MAX(id) with is_called = true, so the next id is MAX(id) + 1.
--   * When the table is empty (e.g. product), use 1 with is_called = false, so the first
--     generated id is 1 rather than 2.
-- pg_get_serial_sequence resolves the real sequence name and copes with the reserved word "order".

SELECT setval(
    pg_get_serial_sequence('customer', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM customer), 1),
    (SELECT COUNT(*) > 0 FROM customer)
);

SELECT setval(
    pg_get_serial_sequence('"order"', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM "order"), 1),
    (SELECT COUNT(*) > 0 FROM "order")
);

SELECT setval(
    pg_get_serial_sequence('product', 'id'),
    GREATEST((SELECT COALESCE(MAX(id), 1) FROM product), 1),
    (SELECT COUNT(*) > 0 FROM product)
);
