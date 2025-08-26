package project1.ares.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GeoLocationService {

    private final WebClient webClient = WebClient.create("http://ip-api.com/json/");

    public Mono<String> getLocation(String ip) {
        return webClient.get()
                .uri(ip)
                .retrieve()
                .bodyToMono(String.class);
    }
}
