package project1.ares.repository;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import project1.ares.model.User;
import project1.ares.util.Regex;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Logger log = LoggerFactory.getLogger(UserRepository.class);

    Mono<User> findByUsername(String username);
    Mono<User> findByEmail(String email);
    Mono<User> findByPhoneNumber(String phoneNumber);

    Mono<Boolean> existsByUsername(String username);
    Mono<Boolean> existsByEmail(String email);
    Mono<Boolean> existsByPhoneNumber(String phoneNumber);

    default Mono<User> findByIdentifier(final String identifier) {

        if (!Regex.isValidEmail(identifier) && !Regex.isValidPhone(identifier) && !Regex.isValidUsername(identifier)) return Mono.error(new IllegalArgumentException("Identificador inválido."));
        return this.findByUsername(identifier.toLowerCase().trim())
                .switchIfEmpty(this.findByEmail(identifier.toLowerCase().trim()))
                .switchIfEmpty(this.findByPhoneNumber(identifier.toLowerCase().trim()));
    }
}
