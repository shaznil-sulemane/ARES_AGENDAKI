package project1.ares.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    private static final String WhatsappURL = "http://localhost:3131/api";
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final WebClient.Builder webClientBuilder;

    public NotificationService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Boolean> sendWhatsappNotification(String number, String message) {
        log.info("Phone Number: {}", number);
        log.info("Message: {}", message);
        if(number == null || message == null) return Mono.just(false);
        if(number.isEmpty() || message.isEmpty()) return Mono.just(false);
        if(!number.matches("^8[4-7][0-9]{7}$") && !number.matches("^2588[4-7][0-9]{7}$")) return Mono.just(false);

        Map<String, Object> body = new HashMap<>();
        body.put("number", number);
        body.put("message", message);

        return webClientBuilder.baseUrl(WhatsappURL).build()
                .post()
                .uri("/send-message")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .toBodilessEntity().flatMap(
                        voidResponseEntity -> {
                            return Mono.just(true);
                        }
                )
                .onErrorResume(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    return Mono.just(false);
                });
    }

}
