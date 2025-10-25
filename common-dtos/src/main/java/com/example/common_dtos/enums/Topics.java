package com.example.common_dtos.enums;

public final class Topics {
    private Topics() {}

    public static final String RESTAURANT_VALIDATE_COMMAND = "restaurant.validate.command";
    public static final String PAYMENT_AUTHORIZE_COMMAND   = "payment.authorize.command";
    public static final String ORDER_SAGA_REPLY            = "order.saga.reply";

    public static final String PAYMENT_CANCEL_COMMAND      = "payment.cancel.command";
    public static final String RESTAURANT_FULFILL_COMMAND  = "restaurant.fulfillment.command";

    public static final String DLT_SUFFIX                  = ".DLT";
}
