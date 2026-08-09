package com.projectx.payment.domain;

/** Mirrors Razorpay's payment `method` field, collapsed to what the product spec asks to show on receipts. */
public enum PaymentMode {
    UPI,
    CARD,
    NETBANKING,
    WALLET,
    OTHER;

    public static PaymentMode fromRazorpayMethod(String method) {
        if (method == null) {
            return OTHER;
        }
        return switch (method.toLowerCase()) {
            case "upi" -> UPI;
            case "card" -> CARD;
            case "netbanking" -> NETBANKING;
            case "wallet" -> WALLET;
            default -> OTHER;
        };
    }
}
