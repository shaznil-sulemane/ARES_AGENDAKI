package project1.ares.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import project1.ares.model.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ServiceRepository extends ReactiveMongoRepository<Service, String> {

    Flux<Service> findAllByActiveTrue();

    default Flux<Service> findAll() {
        return this.findAllByActiveTrue();
    }
    Flux<Service> findServicesByCompanyId(String companyId);
    Flux<Service> findByCompanyId(String companyId);
}
