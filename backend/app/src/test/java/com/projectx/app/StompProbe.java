package com.projectx.app;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * A real STOMP-over-WebSocket client for integration tests: connects to the running server with the
 * JWT on the CONNECT frame, subscribes with a receipt (so "accepted" is observable), and collects
 * pushed frames as raw JSON strings.
 */
final class StompProbe implements AutoCloseable {

    final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
    final AtomicReference<String> lastError = new AtomicReference<>();
    private final WebSocketStompClient client;
    private final StompSession session;

    private StompProbe(WebSocketStompClient client, StompSession session) {
        this.client = client;
        this.session = session;
    }

    static StompProbe connect(int port, String jwt) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        // Hand every frame body to the test as raw text whatever its content-type (the server labels them application/json).
        client.setMessageConverter(new org.springframework.messaging.converter.MessageConverter() {
            @Override
            public Object fromMessage(org.springframework.messaging.Message<?> message, Class<?> targetClass) {
                Object payload = message.getPayload();
                return payload instanceof byte[] bytes ? new String(bytes, java.nio.charset.StandardCharsets.UTF_8) : String.valueOf(payload);
            }

            @Override
            public org.springframework.messaging.Message<?> toMessage(Object payload, org.springframework.messaging.MessageHeaders headers) {
                return null;
            }
        });
        org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler = new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setDaemon(true);
        scheduler.initialize();
        client.setTaskScheduler(scheduler);
        StompHeaders connectHeaders = new StompHeaders();
        if (jwt != null) {
            connectHeaders.add("Authorization", "Bearer " + jwt);
        }
        AtomicReference<StompProbe> holder = new AtomicReference<>();
        StompSessionHandlerAdapter handler = new StompSessionHandlerAdapter() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // ERROR frames from the server land here.
                StompProbe p = holder.get();
                if (p != null) {
                    p.lastError.set(String.valueOf(headers.getFirst("message")));
                }
            }

            @Override
            public void handleTransportError(StompSession s, Throwable ex) {
                StompProbe p = holder.get();
                if (p != null) {
                    p.lastError.set("transport: " + ex);
                }
            }
        };
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws/websocket", (org.springframework.web.socket.WebSocketHttpHeaders) null, connectHeaders, handler)
                .get(10, TimeUnit.SECONDS);
        StompProbe probe = new StompProbe(client, session);
        holder.set(probe);
        return probe;
    }

    /**
     * Subscribes and decides acceptance without relying on receipts (the simple broker does not send RECEIPT
     * frames): an interceptor rejection surfaces as an ERROR frame followed by the server closing the socket,
     * so "still connected and no error after a settle period" means the SUBSCRIBE was accepted. Acceptance is
     * additionally proven end-to-end by the tests that then receive a pushed frame on the same subscription.
     */
    boolean subscribe(String destination) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        session.subscribe(headers, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders h) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders h, Object payload) {
                frames.add((String) payload);
            }
        });
        long deadline = System.currentTimeMillis() + 1500;
        while (System.currentTimeMillis() < deadline) {
            if (!session.isConnected() || lastError.get() != null) {
                return false;
            }
            Thread.sleep(50);
        }
        return session.isConnected() && lastError.get() == null;
    }

    boolean isConnected() {
        return session.isConnected();
    }

    String poll(long seconds) throws InterruptedException {
        return frames.poll(seconds, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        try {
            if (session.isConnected()) {
                session.disconnect();
            }
        } catch (RuntimeException alreadyClosed) {
            // The server may close the socket (after rejecting a SUBSCRIBE) between the isConnected check and the DISCONNECT frame.
            // That race is expected here, not a failure of the code under test.
        } finally {
            client.stop();
        }
    }
}
