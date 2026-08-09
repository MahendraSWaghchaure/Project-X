package com.projectx.payment.gateway;

import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin JSON-over-HTTPS wrapper around Razorpay's REST API (https://razorpay.com/docs/api/) - both
 * the standard Orders/Payments API and RazorpayX's Contacts/Fund Accounts/Payouts API live under
 * the same https://api.razorpay.com/v1 host and the same key_id:key_secret Basic Auth, so one
 * client serves both RazorpayOrderService and RazorpayPayoutClient.
 */
@Component
class RazorpayHttpClient {

    private static final String BASE_URL = "https://api.razorpay.com/v1";
    private static final ParameterizedTypeReference<Map<String, Object>> JSON_MAP = new ParameterizedTypeReference<>() {};

    private final RazorpayProperties properties;

    RazorpayHttpClient(RazorpayProperties properties) {
        this.properties = properties;
    }

    private RestClient client(String keyId, String keySecret) {
        return RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(new JdkClientHttpRequestFactory())
                .defaultHeaders(headers -> headers.setBasicAuth(keyId, keySecret))
                .build();
    }

    Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            return client(properties.getKeyId(), properties.getKeySecret())
                    .post().uri(path).body(body).retrieve().body(JSON_MAP);
        } catch (RestClientResponseException e) {
            throw new PaymentGatewayException("Razorpay API call to " + path + " failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    Map<String, Object> get(String path) {
        try {
            return client(properties.getKeyId(), properties.getKeySecret())
                    .get().uri(path).retrieve().body(JSON_MAP);
        } catch (RestClientResponseException e) {
            throw new PaymentGatewayException("Razorpay API call to " + path + " failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }
}
