package ru.example.websocket.server.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
public class MyWebSocketHandler {

    // WebSocketHandlerAdapter: to handle our web socket handshake, upgrade, and other connection details.
    @Bean
    WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    WebSocketHandler webSocketHandler() {
        return session ->
                session.send(
                        Flux.interval(Duration.ofSeconds(1))
                                .map(Object::toString)
                                .map(session::textMessage)
                ).and(session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(msg -> log.info("Result: " + msg))
                );
    }

    @Bean
    HandlerMapping webSocketURLMapping() {
        SimpleUrlHandlerMapping mapping = new  SimpleUrlHandlerMapping();
        mapping.setUrlMap(Collections.singletonMap("/path", webSocketHandler()));
        mapping.setCorsConfigurations(Collections.singletonMap("*", new CorsConfiguration().applyPermitDefaultValues())); //for Cors
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;

    }
}
