package com.example.demo.domain.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.OrderStatus;
import com.example.demo.domain.valueobject.order.RestaurantId;
import com.example.demo.domain.valueobject.order.Voucher;
import com.example.demo.domain.valueobject.product.ProductId;
import com.example.demo.domain.valueobject.user.UserId;

public class Order {
    private OrderId id;
    private UserId userId;
    private BigDecimal amount;
    private OrderStatus status;
    private Instant createdAt;
    private List<OrderItem> items; // Danh sách mặt hàng
    private Voucher voucher;
    private BigDecimal finalPrice;
    private boolean hasBeenRated;
    private RestaurantId restaurantId;

    public Order(UserId userId, List<OrderItem> items, RestaurantId restaurantId) {
        this.userId = userId;
        this.items = items;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.items.forEach(item -> item.setOrder(this));
        this.hasBeenRated = false;
        this.restaurantId = restaurantId;
        validateItemsExist();
        validateUserIdExists();
        calculateTotalPrice();
        this.finalPrice = this.amount; // Khởi tạo finalPrice bằng amount ban đầu
    }

    public Order(OrderId id, UserId userId, BigDecimal amount, BigDecimal finalPrice, OrderStatus status,
            Instant createdAt,
            List<OrderItem> items, boolean hasBeenRated, RestaurantId restaurantId) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.finalPrice = finalPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
        this.hasBeenRated = hasBeenRated;
        this.restaurantId = restaurantId;
    }

    public OrderId getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public UserId getUserId() {
        return userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Voucher getVoucher() {
        return voucher;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public RestaurantId getRestaurantId() {
        return restaurantId;
    }

    public boolean getHasBeenRated() {
        return hasBeenRated;
    }

    public void setHasBeenRated(boolean hasBeenRated) {
        this.hasBeenRated = hasBeenRated;
    }

    public static class OrderDomainException extends RuntimeException {
        public OrderDomainException(String message) {
            super(message);
        }
    }

    public void applyDiscount(Voucher newVoucher) {
        checkStatusForUpdate();

        this.voucher = newVoucher;
        calculateFinalPrice(); // Tính toán lại giá cuối cùng
    }

    public void confirmPaid() {
        this.status = OrderStatus.PAID;
    }

    public void canceled() {
        this.status = OrderStatus.CANCELLED;
    }

    private void calculateFinalPrice() {
        // 1. Lấy tổng giá trị ban đầu (trước giảm giá)
        BigDecimal basePrice = this.amount; // Giả sử totalPrice đã được tính từ OrderItems

        // 2. Nếu có voucher, áp dụng giảm giá
        if (this.voucher != null) {
            BigDecimal discount = this.voucher.getDiscountAmount();

            // Xử lý giảm giá tiền mặt (Ví dụ đơn giản)
            if (!this.voucher.isPercentage()) {
                this.finalPrice = basePrice.subtract(discount);
            }
            if (this.finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                this.finalPrice = BigDecimal.ZERO;
            }
        } else {
            this.finalPrice = basePrice;
        }
    }

    // thêm items
    public void addItem(List<OrderItem> newItems) {
        checkStatusForUpdate();
        validateNewItem(newItems);

        for (OrderItem newItem : newItems) {
            this.items.add(newItem);
            newItem.setOrder(this);
        }

        calculateTotalPrice();
    }

    // xóa items
    public void removeItems(List<ProductId> productIdsToRemove) {
        checkStatusForUpdate();
        validateItemsList(productIdsToRemove, "Product IDs list for removal cannot be empty.");

        int initialSize = this.items.size();

        // Lọc ra các item có ProductId KHÔNG nằm trong danh sách cần xóa
        this.items = this.items.stream()
                .filter(item -> !productIdsToRemove.contains(item.getProductId()))
                .collect(Collectors.toList());

        int finalSize = this.items.size();

        // Kiểm tra xem có item nào bị xóa không (đảm bảo request hợp lệ)
        if (initialSize == finalSize) {
            throw new OrderDomainException("None of the specified items were found in the order to remove.");
        }

        calculateTotalPrice();
    }

    // kiểm tra tính hợp lệ của user
    private void validateUserIdExists() {
        if (this.userId == null) {
            throw new OrderDomainException("Order must have a valid user ID.");
        }
    }

    private void validateItemsExist() {
        if (this.items == null || this.items.isEmpty()) {
            throw new OrderDomainException("Order must contain at least one item.");
        }
    }

    // kiếm tra tính hợp lệ của 1 item
    private void validateNewItem(List<OrderItem> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            throw new OrderDomainException("New items list cannot be empty for update.");
        }
    }

    // kiếm tra tính hợp lệ của những items trong order item
    private <T> void validateItemsList(List<T> list, String message) {
        if (list == null || list.isEmpty()) {
            throw new OrderDomainException(message);
        }
    }

    // kiển tra trạng thái đơn hàng (Pending / Approved)
    private void checkStatusForUpdate() {
        if (this.status != OrderStatus.PENDING) {
            throw new OrderDomainException(
                    "Order can only be updated when its status is PENDING. Current status: " + this.status);
        }
    }

    // tính toán tổng số tiền 1 đơn hàng
    private void calculateTotalPrice() {
        this.amount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // if (this.amount.compareTo(BigDecimal.ZERO) <= 0) {
        // throw new OrderDomainException("Calculated total price must be greater than
        // zero.");
        // }
    }
    public void validateOrder() {
        //Kiểm tra Item
        validateItemsExist();
        //Kiểm tra User
        validateUserIdExists();
        //Tính tổng giá
        calculateTotalPrice();
    }

    // kiêm tra đánh giá order (rating 1* - 5*)
    public void validateForRating() {
        // 1. Kiểm tra trạng thái: Chỉ cho phép rating khi APPROVED
        if (this.status != OrderStatus.APPROVED) {
            throw new OrderDomainException(
                    "Order can only be rated when its status is APPROVED. Current status: " + this.status);
        }
        // 2. Kiểm tra việc đánh giá lặp lại: Chỉ cho phép rating 1 lần
        if (this.hasBeenRated) {
            throw new OrderDomainException("This order has already been rated.");
        }
    }

}
