// package com.example.common_dtos.enums;

// /**
//  * Trạng thái nghiệp vụ của đơn hàng tại phía Restaurant service.
//  * Giúp đồng bộ với tiến trình tổng thể của Saga:
//  * - Order: PENDING → APPROVED → PAID → COMPLETED / CANCELLING / CANCELLED
//  * - Restaurant: nhận VALIDATE_MENU_ITEMS → phản hồi VALID / INVALID
//  * - Payment: AUTHORIZE / FAILED / REFUNDED / CANCELED
//  *
//  * RestaurantOrderStatus thể hiện trạng thái trung gian khi Restaurant xử lý đơn.
//  */
// public enum RestaurantOrderStatus {

//     /**
//      * Đơn hàng đang chờ xác thực menu tại nhà hàng.
//      * (mapping với SagaStatus.STARTED)
//      */
//     VALIDATION_PENDING,

//     /**
//      * Nhà hàng đã xác thực thành công menu và sẵn sàng xử lý.
//      * (mapping với SagaStatus.RESTAURANT_VALIDATION_OK)
//      */
//     VALIDATED_OK,

//     /**
//      * Xác thực thất bại (menu không hợp lệ, item không tồn tại, hoặc hết hàng).
//      * (mapping với SagaStatus.RESTAURANT_VALIDATION_FAIL)
//      */
//     VALIDATED_FAIL,

//     /**
//      * Sau khi xác thực thành công và thanh toán được authorize.
//      * (mapping với SagaStatus.PAYMENT_AUTHORIZED)
//      */
//     PAYMENT_AUTHORIZED,

//     /**
//      * Thanh toán thất bại từ phía Payment service.
//      * (mapping với SagaStatus.PAYMENT_FAILED)
//      */
//     PAYMENT_FAILED,

//     /**
//      * Nhà hàng bắt đầu chế biến món / chuẩn bị đơn.
//      * (trạng thái trung gian, nội bộ restaurant)
//      */
//     PREPARING,

//     /**
//      * Nhà hàng đã hoàn tất chế biến, đơn sẵn sàng giao.
//      */
//     READY_FOR_DELIVERY,

//     /**
//      * Đơn hàng đã được hoàn tất (mapping với SagaStatus.COMPLETED)
//      */
//     COMPLETED,

//     /**
//      * Đơn hàng đang được rollback (compensating)
//      */
//     COMPENSATING,

//     /**
//      * Rollback (hủy đơn) hoàn tất (mapping với SagaStatus.COMPENSATED)
//      */
//     CANCELLED;

//     /** Trả về true nếu đơn ở trạng thái kết thúc (terminal). */
//     public boolean isTerminal() {
//         return this == COMPLETED || this == CANCELLED || this == VALIDATED_FAIL;
//     }

//     /** Trả về true nếu trạng thái thể hiện lỗi hoặc rollback. */
//     public boolean isFailure() {
//         return this == VALIDATED_FAIL || this == PAYMENT_FAILED || this == CANCELLED;
//     }
// }
