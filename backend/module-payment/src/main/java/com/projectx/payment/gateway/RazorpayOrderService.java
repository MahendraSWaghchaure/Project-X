package com.projectx.payment.gateway;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

/** Razorpay Standard Checkout: create an Order server-side, verify the signature the client gets back once checkout completes. */
@Service
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayOrderService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final RazorpayHttpClient httpClient;
    private final RazorpayProperties properties;

    public RazorpayOrderService(RazorpayHttpClient httpClient, RazorpayProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public String getKeyId() {
        return properties.getKeyId();
    }

    /** amountInRupees is converted to paise (Razorpay's smallest-unit convention) before the call. */
    public String createOrder(BigDecimal amountInRupees, String receiptRef) {
        if (!properties.isEnabled()) {
            throw new PaymentGatewayException("Razorpay is not configured (set projectx.payment.razorpay.enabled=true with real credentials)");
        }
        long amountInPaise = amountInRupees.movePointRight(2).longValueExact();
        Map<String, Object> body = Map.of(
                "amount", amountInPaise,
                "currency", "INR",
                "receipt", receiptRef);
        Map<String, Object> response = httpClient.post("/orders", body);
        return (String) response.get("id");
    }

    /** The three-way HMAC check Razorpay's docs specify: sign("{order_id}|{payment_id}") with key_secret, compare to what checkout.js returned. */
    public boolean verifySignature(String orderId, String razorpayPaymentId, String razorpaySignature) {
        String payload = orderId + "|" + razorpayPaymentId;
        String expected = hmacSha256Hex(payload, properties.getKeySecret());
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                razorpaySignature.getBytes(StandardCharsets.UTF_8));
    }

    /** "card", "upi", "netbanking", "wallet", etc - see PaymentMode.fromRazorpayMethod. */
    public String fetchPaymentMethod(String razorpayPaymentId) {
        Map<String, Object> response = httpClient.get("/payments/" + razorpayPaymentId);
        Object method = response.get("method");
        return method != null ? method.toString() : null;
    }

    /**
     * Razorpay's fetch-order API (https://razorpay.com/docs/api/orders/fetch-with-id/) - the one
     * other call site PaymentReconciliationService needs: Razorpay's own idea of an order's status
     * ("created"/"attempted"/"paid") and how much of it has actually been paid, to compare against
     * what our own Payment row says. Throws PaymentGatewayException (404-shaped, same as any other
     * failed call) if Razorpay has no record of this order id at all - the caller treats that as
     * MISSING_AT_GATEWAY.
     */
    public RazorpayOrderSnapshot fetchOrder(String orderId) {
        if (!properties.isEnabled()) {
            throw new PaymentGatewayException("Razorpay is not configured (set projectx.payment.razorpay.enabled=true with real credentials)");
        }
        Map<String, Object> response = httpClient.get("/orders/" + orderId);
        String status = (String) response.get("status");
        BigDecimal amount = paiseToRupees(response.get("amount"));
        BigDecimal amountPaid = paiseToRupees(response.get("amount_paid"));
        return new RazorpayOrderSnapshot(status, amount, amountPaid);
    }

    private static BigDecimal paiseToRupees(Object paise) {
        if (paise == null) {
            return null;
        }
        return BigDecimal.valueOf(((Number) paise).longValue()).movePointLeft(2);
    }

    /** amount/amountPaid are already converted from paise back to rupees. */
    public record RazorpayOrderSnapshot(String status, BigDecimal amount, BigDecimal amountPaid) {
    }

    private static String hmacSha256Hex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new PaymentGatewayException("Could not compute Razorpay signature", e);
        }
    }
}
