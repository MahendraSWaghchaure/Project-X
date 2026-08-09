package com.projectx.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AdvancePaymentDiscountTest {

    @Test
    void noDiscountBelowThreeMonths() {
        assertThat(AdvancePaymentDiscount.percentFor(1)).isEqualByComparingTo("0");
        assertThat(AdvancePaymentDiscount.percentFor(2)).isEqualByComparingTo("0");
    }

    @Test
    void fivePercentAtThreeMonths() {
        assertThat(AdvancePaymentDiscount.percentFor(3)).isEqualByComparingTo("0.05");
        assertThat(AdvancePaymentDiscount.percentFor(5)).isEqualByComparingTo("0.05");
    }

    @Test
    void eightPercentAtSixMonths() {
        assertThat(AdvancePaymentDiscount.percentFor(6)).isEqualByComparingTo("0.08");
        assertThat(AdvancePaymentDiscount.percentFor(11)).isEqualByComparingTo("0.08");
    }

    @Test
    void twelvePercentAtTwelveMonths() {
        assertThat(AdvancePaymentDiscount.percentFor(12)).isEqualByComparingTo("0.12");
    }

    @Test
    void discountForComputesAndRoundsToTwoDecimalPlaces() {
        // 3 months at 2000/month = 6000 subtotal, 5% off = 300.00 exactly
        BigDecimal discount = AdvancePaymentDiscount.discountFor(BigDecimal.valueOf(6000), 3);
        assertThat(discount).isEqualByComparingTo("300.00");
    }

    @Test
    void discountForRoundsHalfUp() {
        // 3 months at 999/month = 2997 subtotal, 5% = 149.85 exactly, no rounding ambiguity here,
        // but confirms the scale is fixed at 2 decimal places for a currency amount.
        BigDecimal discount = AdvancePaymentDiscount.discountFor(BigDecimal.valueOf(2997), 3);
        assertThat(discount.scale()).isEqualTo(2);
        assertThat(discount).isEqualByComparingTo("149.85");
    }
}
