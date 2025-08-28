package project1.ares.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import project1.ares.model.Plan;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlanRepository extends ReactiveMongoRepository<Plan, String> {
    Mono<Plan> findByName(String name);
    Flux<Plan> findByActiveTrue();
}
