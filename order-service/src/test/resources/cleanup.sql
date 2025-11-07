-- File: src/test/resources/cleanup.sql
-- Script để xóa dữ liệu trước mỗi integration test

-- Disable foreign key checks (PostgreSQL)
SET session_replication_role = 'replica';

-- Xóa dữ liệu từ các bảng
TRUNCATE TABLE order_items CASCADE;
TRUNCATE TABLE orders CASCADE;

-- Enable lại foreign key checks
SET session_replication_role = 'origin';

-- Reset sequences nếu cần
-- ALTER SEQUENCE orders_id_seq RESTART WITH 1;
-- ALTER SEQUENCE order_items_id_seq RESTART WITH 1;