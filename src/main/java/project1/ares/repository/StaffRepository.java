package project1.ares.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import project1.ares.model.Staff;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface StaffRepository extends ReactiveMongoRepository<Staff, String> {
    Mono<Boolean> isAvailable(String staffId, Instant startTime, Instant endTime);
}
