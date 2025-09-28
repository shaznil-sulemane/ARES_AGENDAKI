package project1.ares.repository;

import project1.ares.model.Company;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;

public interface CompanyRepository extends ReactiveMongoRepository<Company, String> {
    Mono<Company> findByName(String name);
    Mono<Company> findByEmail(String email);
    Flux<Company> findCompaniesByOwner(String owner);
    Flux<Company> findByPlanEndDateBeforeAndActiveTrue(LocalDate planEndDate);

    Mono<Boolean> isAvailable(String companyId, Instant startTime, Instant endTime);
}
