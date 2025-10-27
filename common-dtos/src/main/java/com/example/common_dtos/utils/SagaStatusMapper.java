package com.example.common_dtos.utils;

import com.example.common_dtos.enums.*;

/**
 * Mapper trung tâm giữa OrderStatus, RestaurantOrderStatus, PaymentStatus và SagaStatus.
 * KHỚP 100% với enums bạn đang dùng:
 * - OrderStatus: PENDING, APPROVED, PAID, COMPLETED, CANCELLING, CANCELLED
 * - RestaurantOrderStatus: VALIDATION_PENDING, VALIDATED_OK, VALIDATED_FAIL,
 *   PAYMENT_AUTHORIZED, PAYMENT_FAILED, PREPARING, READY_FOR_DELIVERY, COMPLETED,
 *   COMPENSATING, CANCELLED
 * - PaymentStatus: PENDING, AUTHORIZED, FAILED, REFUNDED, CANCELED
 * - SagaStatus: STARTED, RESTAURANT_VALIDATION_OK, RESTAURANT_VALIDATION_FAIL,
 *   PAYMENT_AUTHORIZED, PAYMENT_FAILED, COMPLETED, COMPENSATING, COMPENSATED,
 *   CANCELLED, UNKNOWN
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
            case CANCELLING  -> SagaStatus.COMPENSATING;
            case CANCELLED   -> SagaStatus.CANCELLED;
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
                    -> SagaStatus.PAYMENT_AUTHORIZED;   // sau validate OK
            case COMPLETED            -> SagaStatus.COMPLETED;
            case COMPENSATING         -> SagaStatus.COMPENSATING;
            case CANCELLED            -> SagaStatus.CANCELLED;
        };
    }

    // ===== PAYMENT → SAGA =====
    public static SagaStatus fromPaymentStatus(PaymentStatus status) {
        if (status == null) return SagaStatus.UNKNOWN;
        return switch (status) {
            case PENDING   -> SagaStatus.STARTED;
            case AUTHORIZED-> SagaStatus.PAYMENT_AUTHORIZED;
            case FAILED    -> SagaStatus.PAYMENT_FAILED;
            case REFUNDED  -> SagaStatus.COMPENSATED;
            case CANCELED  -> SagaStatus.CANCELLED;
        };
    }

    // ===== SAGA → ORDER =====
    public static OrderStatus toOrderStatus(SagaStatus saga) {
        if (saga == null) return OrderStatus.CANCELLED; // fallback an toàn
        return switch (saga) {
            case STARTED                   -> OrderStatus.PENDING;
            case RESTAURANT_VALIDATION_OK  -> OrderStatus.APPROVED;
            case PAYMENT_AUTHORIZED        -> OrderStatus.PAID;
            case COMPLETED                 -> OrderStatus.COMPLETED;
            case COMPENSATING              -> OrderStatus.CANCELLING;
            case CANCELLED, COMPENSATED,
                 RESTAURANT_VALIDATION_FAIL, PAYMENT_FAILED
                    -> OrderStatus.CANCELLED;
            default                        -> OrderStatus.CANCELLED; // fallback
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
            case COMPLETED                 -> RestaurantOrderStatus.COMPLETED;
            case COMPENSATING              -> RestaurantOrderStatus.COMPENSATING;
            case CANCELLED, COMPENSATED    -> RestaurantOrderStatus.CANCELLED;
            default                        -> RestaurantOrderStatus.VALIDATION_PENDING;
        };
    }

    // ===== SAGA → PAYMENT =====
    public static PaymentStatus toPaymentStatus(SagaStatus saga) {
        if (saga == null) return PaymentStatus.CANCELED; // fallback
        return switch (saga) {
            case STARTED           -> PaymentStatus.PENDING;
            case PAYMENT_AUTHORIZED-> PaymentStatus.AUTHORIZED;
            case PAYMENT_FAILED    -> PaymentStatus.FAILED;
            case COMPENSATED       -> PaymentStatus.REFUNDED;
            case CANCELLED         -> PaymentStatus.CANCELED;
            default                -> PaymentStatus.CANCELED; // fallback
        };
    }
}
