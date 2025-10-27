-- Dữ liệu sẽ chỉ được chèn nếu bản ghi đó chưa tồn tại trong bảng.

-- ***************************************************************
-- 1. Chèn dữ liệu vào bảng restaurants (5 Nhà hàng)
-- ***************************************************************

-- Nhà hàng 1: Burger Lửa
INSERT INTO restaurants (id, name, address, phone, status, rating, total_ratings, created_at, updated_at)
SELECT 1, 'Burger Lửa - Chi Nhánh Trung Tâm', '99 Đường Hàm Nghi, Quận 1, TP.HCM', '0919001002', 'ACTIVE', 4.7, 1200, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM restaurants WHERE id = 1
);

-- Nhà hàng 2: Sushi Tokyo
INSERT INTO restaurants (id, name, address, phone, status, rating, total_ratings, created_at, updated_at)
SELECT 2, 'Sushi Tokyo', '15 Lê Duẩn, Quận 1, TP.HCM', '0918888888', 'ACTIVE', 4.9, 2500, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM restaurants WHERE id = 2
);

-- Nhà hàng 3: Chè Cô Tám
INSERT INTO restaurants (id, name, address, phone, status, rating, total_ratings, created_at, updated_at)
SELECT 3, 'Chè Cô Tám', '68 Phạm Ngũ Lão, Quận Gò Vấp, TP.HCM', '0903333333', 'ACTIVE', 4.6, 950, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM restaurants WHERE id = 3
);

-- Nhà hàng 4: Món Ý Pasta Mania
INSERT INTO restaurants (id, name, address, phone, status, rating, total_ratings, created_at, updated_at)
SELECT 4, 'Món Ý Pasta Mania', '200 Võ Văn Tần, Quận 3, TP.HCM', '0977444444', 'ACTIVE', 4.3, 700, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM restaurants WHERE id = 4
);

-- Nhà hàng 5: Phở Hùng - Đặc Sản Bò
INSERT INTO restaurants (id, name, address, phone, status, rating, total_ratings, created_at, updated_at)
SELECT 5, 'Phở Hùng - Đặc Sản Bò', '10 Phan Văn Trị, Quận 5, TP.HCM', '0988555555', 'ACTIVE', 4.0, 600, NOW(), NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM restaurants WHERE id = 5
);


-- ***************************************************************
-- 2. Chèn dữ liệu vào bảng menu_items (Sản phẩm)
-- ***************************************************************

-- Lưu ý: Kiểm tra trùng lặp bằng restaurant_id VÀ name (Giả sử là khóa duy nhất)

-- Món ăn cho Nhà hàng ID = 1 (Burger Lửa)
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 1, 'Burger Bò Pho Mát Đặc Biệt', 85000.00, 'Thịt bò Úc, phô mai Cheddar, sốt BBQ', 'Burger', TRUE, NOW(), NOW(), 1
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 1 AND name = 'Burger Bò Pho Mát Đặc Biệt');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 1, 'Burger Gà Giòn Cay', 75000.00, 'Thịt gà giòn, sốt cay đặc biệt', 'Burger', TRUE, NOW(), NOW(), 2
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 1 AND name = 'Burger Gà Giòn Cay');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 1, 'Khoai Tây Phô Mai Tỏi', 55000.00, 'Khoai tây chiên, sốt phô mai và tỏi', 'Side Dish', TRUE, NOW(), NOW(), 3
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 1 AND name = 'Khoai Tây Phô Mai Tỏi');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 1, 'Combo Gia Đình (3 Burger + Nước)', 250000.00, 'Combo tiết kiệm cho 3 người', 'Combo', TRUE, NOW(), NOW(), 4
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 1 AND name = 'Combo Gia Đình (3 Burger + Nước)');


-- Món ăn cho Nhà hàng ID = 2 (Sushi Tokyo)
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 2, 'Set Sushi Cá Hồi (8 miếng)', 180000.00, 'Cá hồi tươi, cơm cuộn', 'Sushi', TRUE, NOW(), NOW(), 5
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 2 AND name = 'Set Sushi Cá Hồi (8 miếng)');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 2, 'Tempura Tôm Lớn', 120000.00, 'Tôm sú tẩm bột chiên giòn', 'Appetizer', TRUE, NOW(), NOW(), 6
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 2 AND name = 'Tempura Tôm Lớn');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 2, 'Cơm Cuộn California', 95000.00, 'Cua, bơ, dưa chuột và mè', 'Maki', TRUE, NOW(), NOW(), 7
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 2 AND name = 'Cơm Cuộn California');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 2, 'Súp Miso Truyền Thống', 35000.00, 'Súp đậu tương, rong biển và đậu phụ', 'Soup', TRUE, NOW(), NOW(), 8
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 2 AND name = 'Súp Miso Truyền Thống');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 2, 'Salad Rong Biển', 60000.00, 'Rong biển Wakame trộn giấm', 'Salad', TRUE, NOW(), NOW(), 9
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 2 AND name = 'Salad Rong Biển');


-- Món ăn cho Nhà hàng ID = 3 (Chè Cô Tám)
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 3, 'Chè Ba Màu Thập Cẩm', 30000.00, 'Đậu xanh, đậu đỏ, sương sáo, nước cốt dừa', 'Chè', TRUE, NOW(), NOW(), 10
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 3 AND name = 'Chè Ba Màu Thập Cẩm');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 3, 'Tào Phớ Hạt Sen', 35000.00, 'Tào phớ mềm với hạt sen và nước đường gừng', 'Tráng Miệng', TRUE, NOW(), NOW(), 11
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 3 AND name = 'Tào Phớ Hạt Sen');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 3, 'Sữa Chua Nếp Cẩm Lạnh', 35000.00, 'Sữa chua dẻo ăn kèm nếp cẩm lên men', 'Tráng Miệng', TRUE, NOW(), NOW(), 12
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 3 AND name = 'Sữa Chua Nếp Cẩm Lạnh');


-- Món ăn cho Nhà hàng ID = 4 (Pasta Mania)
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 4, 'Spaghetti Carbonara Nguyên Bản', 130000.00, 'Mì Ý sốt kem trứng và thịt heo muối (guanciale)', 'Pasta', TRUE, NOW(), NOW(), 13
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 4 AND name = 'Spaghetti Carbonara Nguyên Bản');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 4, 'Pizza Bốn Mùa (Size L)', 250000.00, 'Bốn loại topping khác nhau trên một chiếc pizza', 'Pizza', TRUE, NOW(), NOW(), 14
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 4 AND name = 'Pizza Bốn Mùa (Size L)');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 4, 'Lasagna Bò Phô Mai', 145000.00, 'Mì lá xếp lớp với sốt bò băm và phô mai', 'Pasta', TRUE, NOW(), NOW(), 15
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 4 AND name = 'Lasagna Bò Phô Mai');


-- Món ăn cho Nhà hàng ID = 5 (Phở Hùng)
INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 5, 'Phở Bò Đặc Biệt Tái Nạm Gầu', 80000.00, 'Phở bò tái, nạm, gầu, nước dùng gia truyền', 'Main Course', TRUE, NOW(), NOW(), 16
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 5 AND name = 'Phở Bò Đặc Biệt Tái Nạm Gầu');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 5, 'Phở Gà Lá Chanh', 65000.00, 'Phở gà xé, da giòn, thơm mùi lá chanh', 'Main Course', TRUE, NOW(), NOW(), 17
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 5 AND name = 'Phở Gà Lá Chanh');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 5, 'Bánh Phở Trộn', 70000.00, 'Phở không nước dùng, trộn với sốt và thịt', 'Main Course', TRUE, NOW(), NOW(), 18
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 5 AND name = 'Bánh Phở Trộn');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 5, 'Quẩy Giòn', 10000.00, 'Quẩy chiên giòn ăn kèm phở', 'Side Dish', TRUE, NOW(), NOW(), 19
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 5 AND name = 'Quẩy Giòn');

INSERT INTO menu_items (restaurant_id, name, price, description, category, available, created_at, updated_at, quantity)
SELECT 5, 'Trà Đá', 5000.00, 'Trà đá giải khát', 'Drink', TRUE, NOW(), NOW(), 20
WHERE NOT EXISTS (SELECT 1 FROM menu_items WHERE restaurant_id = 5 AND name = 'Trà Đá');