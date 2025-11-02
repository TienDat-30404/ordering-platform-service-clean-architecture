package com.example.common_dtos.utils;

import com.example.common_dtos.enums.*;

/**
 * Mapper trung tâm giữa OrderStatus, RestaurantOrderStatus, PaymentStatus và SagaStatus.
 * KHỚP 100% với enums hiện tại:
 *
 * - OrderStatus: PENDING, APPROVED, PAID, COMPLETED, CANCELLING, CANCELLED
 * - RestaurantOrderStatus: VALIDATION_PENDING, VALIDATED_OK, VALIDATED_FAIL,
 *   PAYMENT_AUTHORIZED, PAYMENT_FAILED, PREPARING, READY_FOR_DELIVERY, COMPLETED,
 *   COMPENSATING, CANCELLED
 * - PaymentStatus: PENDING, AUTHORIZED, FAILED, REFUND_REQUESTED, REFUND_COMPLETED, CANCELED
 * - SagaStatus: STARTED, RESTAURANT_VALIDATION_OK, RESTAURANT_VALIDATION_FAIL,
 *   PAYMENT_AUTHORIZED, PAYMENT_FAILED, COMPENSATION_STARTED, COMPENSATED,
 *   COMPLETED, CANCELLED, UNKNOWN
 */
public final class SagaStatusMapper {

    private SagaStatusMapper() {}

    // ===== ORDER → SAGA =====
    public static SagaStatus fromOrderStatus(OrderStatus status) {
        if (status == null) return SagaStatus.UNKNOWN;
        return switch (status) {
            case PENDING     -> SagaStatus.STARTED;
            case APPROVED    -> SagaStatus.RESTAURANT_VALIDATION_OK;
            case PAID        -> SagaStatus.PAYMENT_AUTHORIZED;
            case COMPLETED   -> SagaStatus.COMPLETED;
            case CANCELLING  -> SagaStatus.COMPENSATION_STARTED;
            case CANCELLED   -> SagaStatus.CANCELLED;
            case PREPARING -> SagaStatus.PREPARING;
        };
    }

    // ===== RESTAURANT → SAGA =====
    public static SagaStatus fromRestaurantStatus(RestaurantOrderStatus status) {
        if (status == null) return SagaStatus.UNKNOWN;
        return switch (status) {
            case VALIDATION_PENDING   -> SagaStatus.STARTED;
            case VALIDATED_OK         -> SagaStatus.RESTAURANT_VALIDATION_OK;
            case VALIDATED_FAIL       -> SagaStatus.RESTAURANT_VALIDATION_FAIL;
            case PAYMENT_AUTHORIZED   -> SagaStatus.PAYMENT_AUTHORIZED;
            case PAYMENT_FAILED       -> SagaStatus.PAYMENT_FAILED;
            case PREPARING, READY_FOR_DELIVERY
                    -> SagaStatus.PREPARING;
            case COMPLETED            -> SagaStatus.COMPLETED;
            case COMPENSATING         -> SagaStatus.COMPENSATION_STARTED;
            case CANCELLED            -> SagaStatus.CANCELLED;
        };
    }

    // ===== PAYMENT → SAGA =====
    public static SagaStatus fromPaymentStatus(PaymentStatus status) {
        if (status == null) return SagaStatus.UNKNOWN;
        return switch (status) {
            case PENDING           -> SagaStatus.STARTED;
            case AUTHORIZED        -> SagaStatus.PAYMENT_AUTHORIZED;
            case FAILED            -> SagaStatus.PAYMENT_FAILED;
            case REFUND_REQUESTED  -> SagaStatus.COMPENSATION_STARTED;
            case REFUND_COMPLETED  -> SagaStatus.COMPENSATED;
            case CANCELED          -> SagaStatus.CANCELLED;
        };
    }

    // ===== SAGA → ORDER =====
    public static OrderStatus toOrderStatus(SagaStatus saga) {
        if (saga == null) return OrderStatus.CANCELLED;
        return switch (saga) {
            case STARTED                   -> OrderStatus.PENDING;
            case RESTAURANT_VALIDATION_OK  -> OrderStatus.APPROVED;
            case PAYMENT_AUTHORIZED        -> OrderStatus.PAID;
            case COMPLETED                 -> OrderStatus.COMPLETED;
            case COMPENSATION_STARTED      -> OrderStatus.CANCELLING;
            case PREPARING -> OrderStatus.PREPARING;
            case CANCELLED, COMPENSATED,
                 RESTAURANT_VALIDATION_FAIL, PAYMENT_FAILED
                    -> OrderStatus.CANCELLED;
            default                        -> OrderStatus.CANCELLED;
        };
    }

    // ===== SAGA → RESTAURANT =====
    public static RestaurantOrderStatus toRestaurantStatus(SagaStatus saga) {
        if (saga == null) return RestaurantOrderStatus.VALIDATION_PENDING;
        return switch (saga) {
            case STARTED                   -> RestaurantOrderStatus.VALIDATION_PENDING;
            case RESTAURANT_VALIDATION_OK  -> RestaurantOrderStatus.VALIDATED_OK;
            case RESTAURANT_VALIDATION_FAIL-> RestaurantOrderStatus.VALIDATED_FAIL;
            case PAYMENT_AUTHORIZED        -> RestaurantOrderStatus.PAYMENT_AUTHORIZED;
            case PAYMENT_FAILED            -> RestaurantOrderStatus.PAYMENT_FAILED;
            case PREPARING                 -> RestaurantOrderStatus.PREPARING;
            case COMPENSATION_STARTED      -> RestaurantOrderStatus.COMPENSATING;
            case COMPLETED                 -> RestaurantOrderStatus.COMPLETED;
            case COMPENSATED, CANCELLED    -> RestaurantOrderStatus.CANCELLED;
            default                        -> RestaurantOrderStatus.VALIDATION_PENDING;
        };
    }

    // ===== SAGA → PAYMENT =====
    public static PaymentStatus toPaymentStatus(SagaStatus saga) {
        if (saga == null) return PaymentStatus.CANCELED;
        return switch (saga) {
            case STARTED            -> PaymentStatus.PENDING;
            case PAYMENT_AUTHORIZED -> PaymentStatus.AUTHORIZED;
            case PAYMENT_FAILED     -> PaymentStatus.FAILED;
            case COMPENSATION_STARTED -> PaymentStatus.REFUND_REQUESTED;
            case COMPENSATED        -> PaymentStatus.REFUND_COMPLETED;
            case CANCELLED          -> PaymentStatus.CANCELED;
            default                 -> PaymentStatus.CANCELED;
        };
    }
}
