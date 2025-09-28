package project1.ares.repository;

import org.springframework.data.mongodb.repository.Query;
import project1.ares.model.Booking;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import project1.ares.model.BookingStatus;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public interface BookingRepository extends ReactiveMongoRepository<Booking, String> {


    // Consultas otimizadas usando dados desnormalizados
    List<Booking> findByServiceNameAndStatusIn(String serviceName, List<BookingStatus> statuses);
    List<Booking> findByServicePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    List<Booking> findByClientNameContainingIgnoreCase(String clientName);

    // Consulta de conflitos otimizada
    @Query("{ 'companyId': ?0, 'staffId': ?1, 'status': { $in: ['PENDING', 'CONFIRMED'] }, " +
            "$or: [ " +
            "  { 'startTime': { $lt: ?3 }, 'endTime': { $gt: ?2 } }, " +
            "  { 'startTime': { $gte: ?2, $lt: ?3 } }, " +
            "  { 'endTime': { $gt: ?2, $lte: ?3 } } " +
            "] }")
    Flux<List<Booking>> findConflictingBookings(String companyId, String staffId,
                                          Instant startTime, Instant endTime);

    List<Booking> findByClientIdAndStatusIn(String clientId, List<BookingStatus> pending);
}
