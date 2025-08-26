package project1.ares.repository;

import project1.ares.model.Company;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface CompanyRepository extends ReactiveMongoRepository<Company, String> {
    Mono<Company> findByName(String name);
    Mono<Company> findByEmail(String email);
}
