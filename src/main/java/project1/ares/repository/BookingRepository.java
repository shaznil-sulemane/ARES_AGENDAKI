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

    // Consultas básicas por cliente
    Flux<Booking> findByClientId(String clientId);
    Flux<Booking> findByClientIdAndStatus(String clientId, BookingStatus status);
    Flux<Booking> findByClientIdAndStatusIn(String clientId, List<BookingStatus> statuses);

    // Consultas por empresa
    Flux<Booking> findByCompanyId(String companyId);
    Flux<Booking> findByCompanyIdAndStartTimeBetween(String companyId, Instant startDate, Instant endDate);

    // Consultas por status
    Flux<Booking> findByStatus(BookingStatus status);
    Flux<Booking> findByEndTimeBeforeAndStatus(Instant endTime, BookingStatus status);

    // Consultas otimizadas usando dados desnormalizados
    Flux<Booking> findByServiceNameAndStatusIn(String serviceName, List<BookingStatus> statuses);
    Flux<Booking> findByServicePriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
    Flux<Booking> findByClientNameContainingIgnoreCase(String clientName);

    // Consulta de conflitos otimizada
    @Query("{ 'companyId': ?0, 'staffId': ?1, 'status': { $in: ['PENDING', 'CONFIRMED'] }, " +
            "$or: [ " +
            "  { 'startTime': { $lt: ?3 }, 'endTime': { $gt: ?2 } }, " +
            "  { 'startTime': { $gte: ?2, $lt: ?3 } }, " +
            "  { 'endTime': { $gt: ?2, $lte: ?3 } } " +
            "] }")
    Flux<List<Booking>> findConflictingBookings(String companyId, String staffId,
                                          Instant startTime, Instant endTime);

    Flux<Booking> findByStatusInAndEndTimeBefore(
            List<BookingStatus> statuses,
            Instant endTime
    );

}
