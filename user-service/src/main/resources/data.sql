-- Dữ liệu sẽ chỉ được chèn nếu bản ghi đó chưa tồn tại trong bảng.

-- ***************************************************************
-- 1. Chèn dữ liệu vào bảng roles (3 Vai trò)
-- ***************************************************************

-- Vai trò 1: ADMIN (Giả định id = 1)
INSERT INTO roles (id, name)
SELECT 1, 'admin'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE id = 1 OR name = 'admin'
);

-- Vai trò 2: MANAGER (Giả định id = 2)
INSERT INTO roles (id, name)
SELECT 2, 'user'
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE id = 2 OR name = 'user'
);




-- INSERT INTO users (id, name, user_name, password, role_id)
-- SELECT 1, 'Trần Văn A', 'customer', '123456', 2 -- Thay bằng password hash thật
-- WHERE NOT EXISTS (
--     SELECT 1 FROM users WHERE id = 1 OR user_name = 'Trần Văn A'
-- );

-- -- Người dùng 2: Quản lý Cửa hàng Sushi (role_id = 2)
-- INSERT INTO users (id, name, user_name, password, role_id)
-- SELECT 2, 'Lê Thị Admin', 'admin', '123456', 1
-- WHERE NOT EXISTS (
--     SELECT 1 FROM users WHERE id = 2 OR user_name = 'Lê Thị Admin'
-- );

