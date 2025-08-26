package project1.ares.repository;

import project1.ares.model.Booking;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface BookingRepository extends ReactiveMongoRepository<Booking, String> {
    Flux<Booking> findByClientId(String clientId);
}
