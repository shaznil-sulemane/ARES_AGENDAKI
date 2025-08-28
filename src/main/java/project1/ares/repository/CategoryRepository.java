package project1.ares.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import project1.ares.model.Category;
import reactor.core.publisher.Flux;

public interface CategoryRepository extends ReactiveMongoRepository<Category, String> {
    Flux<Category> findByName(String name);
    Flux<Category> findByCompanyId(String companyId);
}
