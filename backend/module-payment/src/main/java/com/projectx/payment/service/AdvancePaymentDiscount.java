package com.projectx.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Percentage-off tiers for prepaying multiple months' fees at once - the more months, the bigger the reward. */
final class AdvancePaymentDiscount {

    private AdvancePaymentDiscount() {
    }

    static BigDecimal percentFor(int monthsCovered) {
        if (monthsCovered >= 12) {
            return BigDecimal.valueOf(0.12);
        }
        if (monthsCovered >= 6) {
            return BigDecimal.valueOf(0.08);
        }
        if (monthsCovered >= 3) {
            return BigDecimal.valueOf(0.05);
        }
        return BigDecimal.ZERO;
    }

    static BigDecimal discountFor(BigDecimal subtotal, int monthsCovered) {
        return subtotal.multiply(percentFor(monthsCovered)).setScale(2, RoundingMode.HALF_UP);
    }
}
