-- Khởi tạo dữ liệu mẫu cho 5 nhà hàng và các món ăn tương ứng
-- Sử dụng ID từ 1 đến 5 cho các nhà hàng

-- -----------------------------------------------------------
-- 1. Chèn dữ liệu vào bảng restaurants (5 Nhà hàng)
-- -----------------------------------------------------------
INSERT INTO restaurants (id, name, address, phone, status, rating, total_ratings, created_at, updated_at) VALUES
(1, 'Burger Lửa - Chi Nhánh Trung Tâm', '99 Đường Hàm Nghi, Quận 1, TP.HCM', '0919001002', 'ACTIVE', 4.7, 1200, NOW(), NOW()),
(2, 'Sushi Tokyo', '15 Lê Duẩn, Quận 1, TP.HCM', '0918888888', 'ACTIVE', 4.9, 2500, NOW(), NOW()),
(3, 'Chè Cô Tám', '68 Phạm Ngũ Lão, Quận Gò Vấp, TP.HCM', '0903333333', 'ACTIVE', 4.6, 950, NOW(), NOW()),
(4, 'Món Ý Pasta Mania', '200 Võ Văn Tần, Quận 3, TP.HCM', '0977444444', 'ACTIVE', 4.3, 700, NOW(), NOW()),
(5, 'Phở Hùng - Đặc Sản Bò', '10 Phan Văn Trị, Quận 5, TP.HCM', '0988555555', 'ACTIVE', 4.0, 600, NOW(), NOW());


-- -----------------------------------------------------------
-- 2. Chèn dữ liệu vào bảng menu_items (Sản phẩm)
-- -----------------------------------------------------------

-- Món ăn cho Nhà hàng ID = 1 (Burger Lửa) - 4 sản phẩm
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at) VALUES
(1, 'Burger Bò Pho Mát Đặc Biệt', 85000.00, 'Thịt bò Úc, phô mai Cheddar, sốt BBQ', 'Burger', TRUE, NOW(), NOW()),
(1, 'Burger Gà Giòn Cay', 75000.00, 'Thịt gà giòn, sốt cay đặc biệt', 'Burger', TRUE, NOW(), NOW()),
(1, 'Khoai Tây Phô Mai Tỏi', 55000.00, 'Khoai tây chiên, sốt phô mai và tỏi', 'Side Dish', TRUE, NOW(), NOW()),
(1, 'Combo Gia Đình (3 Burger + Nước)', 250000.00, 'Combo tiết kiệm cho 3 người', 'Combo', TRUE, NOW(), NOW());

-- Món ăn cho Nhà hàng ID = 2 (Sushi Tokyo) - 5 sản phẩm
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at) VALUES
(2, 'Set Sushi Cá Hồi (8 miếng)', 180000.00, 'Cá hồi tươi, cơm cuộn', 'Sushi', TRUE, NOW(), NOW()),
(2, 'Tempura Tôm Lớn', 120000.00, 'Tôm sú tẩm bột chiên giòn', 'Appetizer', TRUE, NOW(), NOW()),
(2, 'Cơm Cuộn California', 95000.00, 'Cua, bơ, dưa chuột và mè', 'Maki', TRUE, NOW(), NOW()),
(2, 'Súp Miso Truyền Thống', 35000.00, 'Súp đậu tương, rong biển và đậu phụ', 'Soup', TRUE, NOW(), NOW()),
(2, 'Salad Rong Biển', 60000.00, 'Rong biển Wakame trộn giấm', 'Salad', TRUE, NOW(), NOW());

-- Món ăn cho Nhà hàng ID = 3 (Chè Cô Tám) - 3 sản phẩm
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at) VALUES
(3, 'Chè Ba Màu Thập Cẩm', 30000.00, 'Đậu xanh, đậu đỏ, sương sáo, nước cốt dừa', 'Chè', TRUE, NOW(), NOW()),
(3, 'Tào Phớ Hạt Sen', 35000.00, 'Tào phớ mềm với hạt sen và nước đường gừng', 'Tráng Miệng', TRUE, NOW(), NOW()),
(3, 'Sữa Chua Nếp Cẩm Lạnh', 35000.00, 'Sữa chua dẻo ăn kèm nếp cẩm lên men', 'Tráng Miệng', TRUE, NOW(), NOW());

-- Món ăn cho Nhà hàng ID = 4 (Pasta Mania) - 3 sản phẩm
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at) VALUES
(4, 'Spaghetti Carbonara Nguyên Bản', 130000.00, 'Mì Ý sốt kem trứng và thịt heo muối (guanciale)', 'Pasta', TRUE, NOW(), NOW()),
(4, 'Pizza Bốn Mùa (Size L)', 250000.00, 'Bốn loại topping khác nhau trên một chiếc pizza', 'Pizza', TRUE, NOW(), NOW()),
(4, 'Lasagna Bò Phô Mai', 145000.00, 'Mì lá xếp lớp với sốt bò băm và phô mai', 'Pasta', TRUE, NOW(), NOW());

-- Món ăn cho Nhà hàng ID = 5 (Phở Hùng) - 5 sản phẩm
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at) VALUES
(5, 'Phở Bò Đặc Biệt Tái Nạm Gầu', 80000.00, 'Phở bò tái, nạm, gầu, nước dùng gia truyền', 'Main Course', TRUE, NOW(), NOW()),
(5, 'Phở Gà Lá Chanh', 65000.00, 'Phở gà xé, da giòn, thơm mùi lá chanh', 'Main Course', TRUE, NOW(), NOW()),
(5, 'Bánh Phở Trộn', 70000.00, 'Phở không nước dùng, trộn với sốt và thịt', 'Main Course', TRUE, NOW(), NOW()),
(5, 'Quẩy Giòn', 10000.00, 'Quẩy chiên giòn ăn kèm phở', 'Side Dish', TRUE, NOW(), NOW()),
(5, 'Trà Đá', 5000.00, 'Trà đá giải khát', 'Drink', TRUE, NOW(), NOW());