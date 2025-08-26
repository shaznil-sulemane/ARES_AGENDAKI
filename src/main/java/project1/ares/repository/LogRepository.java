package project1.ares.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import project1.ares.model.Log;
import reactor.core.publisher.Flux;

import java.time.Instant;

public interface LogRepository extends ReactiveMongoRepository<Log, String> {
    Flux<Log> findByUser(String user);
    Flux<Log> findByDateAfter(Instant date);
    Flux<Log> findByDateBefore(Instant date);
    Flux<Log> findByDateBetween(Instant from, Instant to);
}