package project1.ares.service;

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
    private final WebClient.Builder webClientBuilder;

    public NotificationService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Boolean> sendWhatsappNotification(String number, String message) {
        if(number == null || message == null) return Mono.just(false);
        if(number.isEmpty() || message.isEmpty()) return Mono.just(false);
        if(!number.matches("^8[4-7][0-9]{7}$")) return Mono.just(false);

        Map<String, Object> body = new HashMap<>();
        body.put("number", "+258" + number);
        body.put("message", message);

        return webClientBuilder.baseUrl(WhatsappURL).build()
                .post()
                .uri("/send-message")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .retrieve()
                .bodyToMono(Boolean.class);
    }

}
