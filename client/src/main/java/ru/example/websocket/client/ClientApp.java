package ru.example.websocket.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootApplication
public class ClientApp {

    public static void main(String[] args) {
        SpringApplication.run(ClientApp.class, args);
    }

    @Value("${app.client.url:ws://localhost:8080/path}")
    private String serverURI;


    Mono<Void> wsConnectNetty() {
        WebSocketClient client = new ReactorNettyWebSocketClient();
        return client.execute(
                URI.create(serverURI),
                session -> session
                        .receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .take(8)
                        .doOnNext(number ->
                                log.info("Session id: " + session.getId() + " execute: " + number)
                        )
                        .flatMap(txt ->
                                session.send(
                                        Mono.just(session.textMessage(txt))
                                )
                        )
                        .doOnSubscribe(subscriber ->
                                log.info("Session id: " + session.getId() + " open connection")
                        )
                        .doFinally(signalType -> {
                            session.close();
                            log.info("Session id: " + session.getId() + " close connection");
                        })
                        .then()
        );
    }

    // для тестов
    @Bean
    ApplicationRunner appRunner() {
        return args -> {
            final CountDownLatch latch = new CountDownLatch(2);
            Flux.merge(
                            Flux.range(0, 2)
                                    .subscribeOn(Schedulers.single())
                                    .map(n -> wsConnectNetty()
                                            .doOnTerminate(latch::countDown))
                                    .parallel()
                    )
                    .subscribe();

            latch.await(20, TimeUnit.SECONDS);
        };
    }
}
