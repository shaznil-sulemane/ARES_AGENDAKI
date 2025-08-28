package project1.ares.controller;

import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import project1.ares.config.ApiResponse;
import project1.ares.model.Plan;
import project1.ares.repository.PlanRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/plan")
public class PlanController {

    private final PlanRepository planRepository;

    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<Plan>>>> getPlans() {
        return planRepository.findByActiveTrue()
                .sort(Comparator.comparing(Plan::getPosition))
                .map(plan -> plan)
                .collectList()
                .map(plans -> {
                    if (plans.isEmpty()) {
                        return ResponseEntity.status(404).body(
                                new ApiResponse<>(
                                        false,
                                        "Nenhuma empresa encontrada",
                                        null
                                )
                        );
                    }
                    return ResponseEntity.ok(
                            new ApiResponse<>(
                                    true,
                                    "Empresas encontradas com sucesso",
                                    plans
                            )
                    );
                });
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Plan>>> createPlan(@RequestBody PlanRequest plan) {
        if (plan == null) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(false, "Plano é nulo", null)));
        }

        if (plan.getPrice() < 0.0) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(false, "Preço negativo", null)));
        }

        if (plan.getName().isEmpty() || plan.getTitle().isEmpty()
                || plan.getDescription().isEmpty() || plan.getDuration() == null) {
            return Mono.just(ResponseEntity
                    .badRequest()
                    .body(new ApiResponse<>(false, "Request com valores em falta", null)));
        }

        return planRepository.findByName(plan.getName())
                .flatMap(existingPlan -> Mono.just(
                        ResponseEntity
                                .badRequest()
                                .body(new ApiResponse<Plan>(false, "Plano já existe", null))
                ))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            Plan newPlan = new Plan();
                            newPlan.setName(plan.getName());
                            newPlan.setTitle(plan.getTitle());
                            newPlan.setPrice(BigDecimal.valueOf(plan.getPrice()));
                            newPlan.setDescription(plan.getDescription());
                            newPlan.setDuration(plan.getDuration());
                            newPlan.setFeatures(plan.getFeatures());
                            newPlan.setBadge(plan.getBadge());
                            newPlan.setActive(plan.isActive());
                            newPlan.setPosition(plan.getPosition());
                            return planRepository.save(newPlan)
                                    .map(savedPlan -> ResponseEntity.ok(new ApiResponse<>(true, "Plano salvo com sucesso.", savedPlan)));
                        })
                );
    }

    @Data
    public static class PlanRequest {
        private String name = "";
        private double price = 0.0;
        private String title = "";
        private int position = 0;
        private String description = "";
        private boolean active = true;
        private Plan.Duration duration = Plan.Duration.MENSAL;
        private List<String> features = new ArrayList<>();
        private String badge;
    }
}
