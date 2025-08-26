package project1.ares.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import project1.ares.model.Counter;
import reactor.core.publisher.Mono;

import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Service
@RequiredArgsConstructor
public class SequenceGeneratorService {

    private final ReactiveMongoOperations mongoOperations;

    public Mono<String> generateId(String key, String prefix, int length) {
        return mongoOperations.findAndModify(
                        query(where("_id").is(key)),
                        new Update().inc("seq", 1),
                        Counter.class
                )
                .flatMap(counter -> Mono.just(formatId(counter.getSeq(), prefix, length)))
                .switchIfEmpty(
                        // cria o primeiro contador se não existir
                        mongoOperations.save(new Counter(key, 1))
                                .map(counter -> formatId(1, prefix, length))
                );
    }

    private String formatId(long seq, String prefix, int length) {
        return String.format("%s%0" + length + "d", prefix, seq);
    }

    // Variações prontas para coleções comuns
    public Mono<String> generateUserId() {
        return generateId("user", "U", 7);
    }

    // Variações prontas para coleções comuns
    public Mono<String> generateCompanyId() {
        return generateId("company", "CP", 9);
    }

    // Variações prontas para coleções comuns
    public Mono<String> generateServiceId() {
        return generateId("service", "SV", 7);
    }

    // Variações prontas para coleções comuns
    public Mono<String> generateStaffId() {
        return generateId("staff", "ST", 7);
    }

    // Variações prontas para coleções comuns
    public Mono<String> generateBookingId() {
        return generateId("booking", "B", 7);
    }

    public Mono<String> generateTransactionId() {
        return generateId("transaction", "TX", 12);
    }

    public Mono<String> generateWalletId() {
        return generateId("wallet", "W", 8);
    }
}
