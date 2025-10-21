package com.example.demo.adapters.in.controller;

import java.net.Authenticator;
import java.util.List;
import java.util.Map;

import com.example.demo.order_messaging.publisher.OrderEventPublisherAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.adapters.out.config.AuthenticatedUser;
import com.example.demo.application.dto.command.AddItemsCommand;
import com.example.demo.application.dto.command.ApplyVoucherCommand;
import com.example.demo.application.dto.command.CreateOrderCommand;
import com.example.demo.application.dto.command.RateOrderCommand;
import com.example.demo.application.dto.command.RemoveItemsCommand;
import com.example.demo.application.dto.output.OrderStatisticsResponse;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.dto.query.GetOrdersByCustomerQuery;
import com.example.demo.application.ports.input.AddItemsUseCase;
import com.example.demo.application.ports.input.ApplyVoucherUseCase;
import com.example.demo.application.ports.input.CreateOrderUseCase;
import com.example.demo.application.ports.input.GetAllOrdersUseCase;
import com.example.demo.application.ports.input.GetOrderHistoryUseCase;
import com.example.demo.application.ports.input.OrderStatisticsUseCase;
import com.example.demo.application.ports.input.RateOrderUseCase;
import com.example.demo.application.ports.input.RemoveItemsUseCase;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.user.UserId;

import com.example.demo.application.orchestrator.OrderOrchestratorService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderEventPublisherAdapter publisher;
    private final GetAllOrdersUseCase getAllOrdersUseCase;
    private final CreateOrderUseCase createOrderUseCase;
    private final AddItemsUseCase addItemsToOrderUseCase;
    private final RemoveItemsUseCase removeItemsUseCase;
    private final GetOrderHistoryUseCase getOrderHistoryUseCase;
    private final OrderStatisticsUseCase orderStatisticsUseCase;
    private final ApplyVoucherUseCase applyVoucherUseCase;
    private final RateOrderUseCase rateOrderUseCase;
    private final AuthenticatedUser authenticatedUser;

    private final OrderOrchestratorService orchestratorService;

    // danh sách đơn hàng
    @GetMapping
    public ResponseEntity<List<TrackOrderResponse>> getAllOrders() {
        List<TrackOrderResponse> orders = getAllOrdersUseCase.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    // tạo đơn hàng
    @PostMapping
    // public ResponseEntity<TrackOrderResponse> createOrder(
    // @RequestBody CreateOrderCommand command) {
    // Long userId = authenticatedUser.getUserId();
    // TrackOrderResponse response = createOrderUseCase.createOrder(command,
    // userId);
    // return ResponseEntity.ok(response);
    // }
    public ResponseEntity<TrackOrderResponse> createOrder(@RequestBody CreateOrderCommand command) {
        Long userId = authenticatedUser.getUserId();
        TrackOrderResponse res = createOrderUseCase.createOrder(command, userId); // lưu PENDING

        orchestratorService.startCreateOrderSagaFromCommand(
                res.getId().toString(),
                res.getRestaurantId(),
                command.getItems());

        return ResponseEntity.ok(res);
    }

    // // cập nhật đơn hàng - thêm sản phẩm
    @PutMapping("/{orderId}/add-items")
    public ResponseEntity<TrackOrderResponse> addItemsToOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody AddItemsCommand command) {

        Long userId = authenticatedUser.getUserId();
        command.setOrderId(orderId);
        TrackOrderResponse response = addItemsToOrderUseCase.addItems(command, userId);
        return ResponseEntity.ok(response);
    }

    // cập nhât đơn hàng - xóa sản phẩm
    @PatchMapping("/{orderId}/remove-items")
    public ResponseEntity<TrackOrderResponse> removeItemsFromOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody RemoveItemsCommand command) {
        Long userId = authenticatedUser.getUserId();
        TrackOrderResponse response = removeItemsUseCase.removeItems(command, new UserId(userId), new OrderId(orderId));
        return ResponseEntity.ok(response);
    }

    // lấy tất cả đơn hàng của 1 user dựa vào userID
    @GetMapping("/history-orders")
    public ResponseEntity<List<TrackOrderResponse>> getOrdersByUser() {
        Long userId = authenticatedUser.getUserId();
        List<TrackOrderResponse> response = getOrderHistoryUseCase.getOrdersByCustomer(userId);
        return ResponseEntity.ok(response);
    }

    // thống kê tất cả đơn hàng ( tổng đơn, tổng tiền, trung bình sô tiền / mỗi đơn
    // hàng)
    @GetMapping("/statistics")
    public ResponseEntity<OrderStatisticsResponse> getOrderStatistics() {

        OrderStatisticsResponse statistics = orderStatisticsUseCase.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    // áp dụng voucher cho đơn hàng
    @PatchMapping("/{orderId}/voucher")
    public ResponseEntity<Void> applyVoucher(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> request) {

        String voucherCode = request.get("voucherCode");
        if (voucherCode == null) {
            return ResponseEntity.badRequest().build();
        }

        ApplyVoucherCommand command = ApplyVoucherCommand.builder()
                .orderId(orderId)
                .voucherCode(voucherCode)
                .build();

        applyVoucherUseCase.applyVoucher(command);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content
    }

    // đánh giá đơn hàng
    @PostMapping("/rating")
    public ResponseEntity<Void> rateOrder(
            @RequestBody RateOrderCommand request) {
        Long userId = authenticatedUser.getUserId();
        RateOrderCommand command = RateOrderCommand.builder()
                .orderId(request.getOrderId())
                .score(request.getScore())
                .comment(request.getComment())
                .build();

        rateOrderUseCase.rateOrder(command, new UserId(userId));

        return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created
    }   

    @GetMapping("/test")
    public String test() {
        return "Hello World 1234567777";
    }
}