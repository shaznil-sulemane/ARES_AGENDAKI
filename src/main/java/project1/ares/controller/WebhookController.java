package project1.ares.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import project1.ares.config.ApiResponse;
import project1.ares.service.SequenceGeneratorService;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping
public class WebhookController {


    private final SequenceGeneratorService sequenceGeneratorService;
    private final WebClient.Builder webClientBuilder;

    private static final String API_URL = "https://paysuite.tech/api/v1";
    private static final String AUTH_TOKEN = "490|Y97U2s8OLOMtywgnwlRrX0FS9P6KH1x2BFQa2pFabe693b8d";


    public WebhookController(SequenceGeneratorService sequenceGeneratorService, WebClient.Builder webClientBuilder) {
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.webClientBuilder = webClientBuilder;
    }

    @PostMapping("/pay")
    public Mono<ResponseEntity<ApiResponse<String>>> pay(@RequestBody Map<String, String> bodyReq) {
        String amount = (String) bodyReq.getOrDefault("amount", "30");
        String description = (String) bodyReq.getOrDefault("description", "Pedido padrão");

        // Se reference não fornecida → gerar sequencial
        Mono<String> referenceMono;
        if (bodyReq.containsKey("reference")) {
            referenceMono = Mono.just((String) bodyReq.get("reference"));
        } else {
            referenceMono = sequenceGeneratorService.generateId("invoice", "INV", 7);
        }

        return referenceMono.flatMap(reference -> {
            Map<String, Object> body = Map.of(
                    "amount", amount,
                    "reference", reference,
                    "description", description,
                    "return_url", "http://192.168.0.103:5173/dashboard/manager",
                    "callback_url", "https://bbea15be054b.ngrok-free.app/webhooks/paysuite"
            );

            return webClientBuilder.baseUrl(API_URL).build()
                    .post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + AUTH_TOKEN)
                    .body(BodyInserters.fromValue(body))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .flatMap(response -> {
                        Map<String, Object> data = (Map<String, Object>) response.get("data");
                        String checkoutUrl = (String) data.get("checkout_url"); // pega o checkout_url
                        String paymentId = (String) data.get("id");             // pega o id do pagamento
                        //String reference = (String) data.get("reference");      // pega a referência
                        //String amount = (String) data.get("amount");            // pega o valor


                        // Aqui você pode salvar paymentId + reference no banco, etc.
                        System.out.println(checkoutUrl);
                        return Mono.just(ResponseEntity.ok(new ApiResponse<>(true, "Pagamento gerado", checkoutUrl)));
                    });

        });

    }

    @PostMapping(path = "/webhooks/paysuite", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Void>> webhook(@RequestBody Map<String, Object> event) {
        System.out.println("Recebido webhook: " + event);

        String paymentId = (String) event.get("payment_id");

        return webClientBuilder.baseUrl(API_URL).build()
                .get()
                .uri("/payments/{id}", paymentId != null ? paymentId : "REQ6xcz")
                .header("Authorization", "Bearer " + AUTH_TOKEN)
                .retrieve()
                .bodyToMono(Map.class)
                .map(payment -> {
                    System.out.println("Pagamento atualizado: " + payment);
                    String status = (String) payment.get("status");
                    if ("paid".equalsIgnoreCase(status)) {
                        // marque o pedido como pago e libere o serviço
                    }
                    return ResponseEntity.ok().build();
                });
    }
}
