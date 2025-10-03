package project1.ares.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import project1.ares.config.BookingException;
import project1.ares.dto.create.BookingCREATE;
import project1.ares.model.*;
import project1.ares.repository.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;


@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final CompanyRepository companyRepository;
    private final StaffRepository staffRepository;
    private final EmailService emailService;

    public BookingService(BookingRepository bookingRepository,
                          UserRepository userRepository,
                          ServiceRepository serviceRepository,
                          CompanyRepository companyRepository,
                          StaffRepository staffRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.serviceRepository = serviceRepository;
        this.companyRepository = companyRepository;
        this.staffRepository = staffRepository;
        this.emailService = emailService;
    }

    // ==================== CRIAR AGENDAMENTO ====================

    public Mono<Booking> createBooking(BookingCREATE bookingCreate) {
        return validateAndCreateBooking(bookingCreate)
                .flatMap(this::saveBooking)
                .flatMap(this::sendConfirmationEmail)
                .doOnError(error -> log.error("Erro ao criar agendamento: {}", error.getMessage()));
    }

    private Mono<Booking> validateAndCreateBooking(BookingCREATE bookingCreate) {
        Mono<User> clientMono = userRepository.findById(bookingCreate.getClientId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Cliente não encontrado")));

        Mono<project1.ares.model.Service> serviceMono = serviceRepository.findById(bookingCreate.getServiceId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Serviço não encontrado")));

        return Mono.zip(clientMono, serviceMono)
                .flatMap(tuple -> {
                    User client = tuple.getT1();
                    project1.ares.model.Service service = tuple.getT2();

                    Instant startTime = bookingCreate.getStartTime();
                    Instant endTime = startTime.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);

                    return checkTimeSlotAvailability(bookingCreate.getCompanyId(),
                            bookingCreate.getStaffId(),
                            startTime,
                            endTime)
                            .then(Mono.fromCallable(() ->
                                    Booking.createFromEntities(client, service, bookingCreate, bookingCreate.getCreatedBy())
                            ));
                });
    }

    private Mono<Void> checkTimeSlotAvailability(String companyId, String staffId,
                                                 Instant startTime, Instant endTime) {
        Mono<Void> companyCheck = checkCompanyAvailability(companyId, startTime, endTime);
        Mono<Void> staffCheck = (staffId != null)
                ? checkStaffAvailability(staffId, startTime, endTime)
                : Mono.empty();
        Mono<Void> bookingCheck = checkBookingConflicts(companyId, staffId, startTime, endTime);

        return Mono.when(companyCheck, staffCheck, bookingCheck);
    }

    private Mono<Void> checkCompanyAvailability(String companyId, Instant startTime, Instant endTime) {
        return companyRepository.findById(companyId)
                .flatMap(company -> {
                    if (!company.isOpenAt(startTime)) {
                        return Mono.<Void>error(new IllegalArgumentException("Empresa indisponível nesse horário"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> checkStaffAvailability(String staffId, Instant startTime, Instant endTime) {
        return staffRepository.findById(staffId)
                .flatMap(staff -> {
                    ZoneId zoneId = ZoneId.systemDefault();
                    DayOfWeek dayOfWeek = startTime.atZone(zoneId).getDayOfWeek();

                    if (!staff.isWorkingAt(startTime, dayOfWeek)) {
                        return Mono.<Void>error(new IllegalArgumentException("Funcionário indisponível nesse horário"));
                    }
                    return Mono.empty();
                });
    }


    private Mono<Void> checkBookingConflicts(String companyId, String staffId,
                                             Instant startTime, Instant endTime) {
        return bookingRepository.findConflictingBookings(companyId, staffId, startTime, endTime)
                .collectList()
                .flatMap(conflictingBookings -> {
                    if (!conflictingBookings.isEmpty()) {
                        return Mono.error(new BookingException(
                                "Horário indisponível - Já existe agendamento neste período"));
                    }
                    return Mono.empty();
                });
    }

    private Mono<Booking> saveBooking(Booking booking) {
        return bookingRepository.save(booking)
                .doOnSuccess(saved -> log.info("Agendamento criado com sucesso: {}", saved.getId()));
    }

    private Mono<Booking> sendConfirmationEmail(Booking booking) {
        Map<String, String> emailParams = Map.of(
                "clientName", booking.getClientName(),
                "clientEmail", booking.getClientEmail(),
                "serviceName", booking.getServiceName(),
                "startTime", formatDateTime(booking.getStartTime()),
                "endTime", formatDateTime(booking.getEndTime()),
                "price", booking.getServicePrice().toString(),
                "currency", booking.getCurrency(),
                "bookingId", booking.getId()
        );

        return emailService.sendEmail(
                        booking.getClientEmail(),
                        "Confirmação de Agendamento",
                        1,
                        emailParams
                )
                .map(emailSent -> {
                    if (emailSent) {
                        booking.setEmailConfirmationSent(true);
                        booking.setUpdatedAt(Instant.now());
                        bookingRepository.save(booking);
                        log.info("Email de confirmação enviado para: {}", booking.getClientEmail());
                    } else {
                        log.warn("Falha ao enviar email de confirmação para: {}", booking.getClientEmail());
                    }
                    return booking;
                })
                .onErrorResume(error -> {
                    log.error("Erro ao enviar email de confirmação: {}", error.getMessage());
                    return Mono.just(booking);
                });
    }

    // ==================== BUSCAR AGENDAMENTOS ====================

    public Mono<Flux<Booking>> getClientBookings(String clientId) {
        return Mono.fromCallable(() ->
                bookingRepository.findByClientIdAndStatusIn(
                        clientId,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                )
        );
    }

    public Flux<Booking> getClientBookingsByStatus(String clientId, BookingStatus status) {
        return bookingRepository.findByClientIdAndStatusIn(clientId, List.of(status));
    }

    public Flux<Booking> getAllClientBookings(String clientId) {
        return bookingRepository.findByClientId(clientId);
    }

    public Flux<Booking> getCompanyBookings(String companyId) {
        return bookingRepository.findByCompanyId(companyId);
    }

    public Flux<Booking> getBookingsByDateRange(String companyId, Instant startDate, Instant endDate) {
        return bookingRepository.findByCompanyIdAndStartTimeBetween(companyId, startDate, endDate);
    }

    public Mono<List<Booking>> getBookingsByServiceName(String serviceName) {
        return bookingRepository.findByServiceNameAndStatusIn(serviceName,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
                .collectList();
    }

    public Mono<List<Booking>> getBookingsInPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return bookingRepository.findByServicePriceBetween(minPrice, maxPrice)
                .collectList();
    }

    public Mono<Booking> getBookingById(String bookingId) {
        return bookingRepository.findById(bookingId);
    }

    // ==================== GERENCIAR AGENDAMENTOS ====================

    public Mono<Booking> cancelBooking(String bookingId, String cancelledBy, String reason) {
        return bookingRepository.findById(bookingId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agendamento não encontrado")))
                .flatMap(booking -> {
                    if (!booking.canBeCancelled()) {
                        return Mono.error(new BookingException("Agendamento não pode ser cancelado"));
                    }

                    booking.setStatus(BookingStatus.CANCELED);
                    booking.setCancelledAt(Instant.now());
                    booking.setCancelledBy(cancelledBy);
                    booking.setCancellationReason(reason);
                    booking.setUpdatedAt(Instant.now());

                    return bookingRepository.save(booking);
                })
                .doOnSuccess(cancelled -> log.info("Agendamento cancelado: {}", cancelled.getId()));
    }

    public Mono<Booking> confirmBooking(String bookingId, String confirmedBy) {
        return bookingRepository.findById(bookingId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Agendamento não encontrado")))
                .flatMap(booking -> {
                    if (booking.getStatus() != BookingStatus.PENDING) {
                        return Mono.error(new IllegalArgumentException(
                                "Apenas agendamentos pendentes podem ser confirmados"
                        ));
                    }

                    if (booking.isInPast()) {
                        return Mono.error(new IllegalArgumentException(
                                "Não é possível confirmar agendamento que já passou"
                        ));
                    }

                    booking.setStatus(BookingStatus.CONFIRMED);
                    booking.setConfirmedAt(Instant.now());
                    booking.setUpdatedAt(Instant.now());

                    return bookingRepository.save(booking);
                })
                .doOnSuccess(confirmed ->
                        log.info("Agendamento {} confirmado por {}", bookingId, confirmedBy)
                );
    }

    public Mono<Long> updateExpiredBookingsToCompleted() {
        Instant now = Instant.now();

        return bookingRepository.findByStatusInAndEndTimeBefore(
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED),
                        now
                )
                .flatMap(booking -> {
                    booking.setStatus(BookingStatus.COMPLETED);
                    booking.setUpdatedAt(now);
                    return bookingRepository.save(booking);
                })
                .count()
                .doOnSuccess(count ->
                        log.info("Atualizados {} agendamentos expirados para COMPLETED", count)
                );
    }

    // ==================== ESTATÍSTICAS ====================

    public Mono<BookingStats> getBookingStats(String companyId) {
        return bookingRepository.findByCompanyId(companyId)
                .collectList()
                .map(bookings -> {
                    Map<BookingStatus, Long> statusCountMap = bookings.stream()
                            .collect(Collectors.groupingBy(
                                    Booking::getStatus,
                                    Collectors.counting()
                            ));

                    List<BookingStatusCount> statusCounts = statusCountMap.entrySet().stream()
                            .map(entry -> new BookingStatusCount(entry.getKey(), entry.getValue()))
                            .toList();

                    long totalBookings = bookings.size();
                    long pendingCount = statusCountMap.getOrDefault(BookingStatus.PENDING, 0L);
                    long confirmedCount = statusCountMap.getOrDefault(BookingStatus.CONFIRMED, 0L);
                    long completedCount = statusCountMap.getOrDefault(BookingStatus.COMPLETED, 0L);
                    long canceledCount = statusCountMap.getOrDefault(BookingStatus.CANCELED, 0L);

                    BigDecimal totalRevenue = bookings.stream()
                            .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                            .map(Booking::getServicePrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new BookingStats(
                            statusCounts,
                            totalBookings,
                            pendingCount,
                            confirmedCount,
                            completedCount,
                            canceledCount,
                            totalRevenue
                    );
                })
                .doOnSuccess(stats -> log.info(
                        "Estatísticas calculadas para empresa {}: {} total, {} pendentes, {} confirmados, {} completados, {} cancelados, Receita: {}",
                        companyId, stats.getTotalBookings(), stats.getPendingCount(),
                        stats.getConfirmedCount(), stats.getCompletedCount(),
                        stats.getCanceledCount(), stats.getTotalRevenue()
                ))
                .defaultIfEmpty(new BookingStats(
                        List.of(),
                        0L, 0L, 0L, 0L, 0L,
                        BigDecimal.ZERO
                ));
    }

    // ==================== SLOTS DISPONÍVEIS (MÉTODO ÚNICO OTIMIZADO) ====================

    /**
     * Retorna os slots disponíveis para agendamento
     * MÉTODO ÚNICO - Remove duplicação
     */
    public Flux<AvailableSlot> getAvailableSlots(String companyId, String staffId,
                                                 Instant date, String serviceId) {
        return serviceRepository.findById(serviceId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Serviço não encontrado")))
                .flatMapMany(service -> companyRepository.findById(companyId)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Empresa não encontrada")))
                        .flatMapMany(company -> generateAvailableSlots(company, staffId, date, service))
                )
                .doOnComplete(() -> log.info("Slots calculados - empresa: {}, data: {}", companyId, date));
    }

    private Flux<AvailableSlot> generateAvailableSlots(Company company, String staffId,
                                                       Instant date, project1.ares.model.Service service) {
        ZoneId zoneId = ZoneId.of(company.getTimeZone());
        LocalDate localDate = date.atZone(zoneId).toLocalDate();
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();

        // Verificar se a empresa está aberta neste dia
        Company.BusinessHours businessHours = company.getBusinessHoursForDay(dayOfWeek);
        if (businessHours.isClosed()) {
            return Flux.empty();
        }

        // Definir início e fim do dia
        ZonedDateTime dayStart = localDate.atTime(businessHours.getOpenTime()).atZone(zoneId);
        ZonedDateTime dayEnd = localDate.atTime(businessHours.getCloseTime()).atZone(zoneId);

        // Verificar antecedência mínima
        Instant minBookingTime = Instant.now().plus(
                company.getBookingSettings().getMinAdvanceBookingHours(),
                ChronoUnit.HOURS
        );

        if (dayEnd.toInstant().isBefore(minBookingTime)) {
            return Flux.empty();
        }

        // Ajustar início se necessário
        Instant effectiveStart = dayStart.toInstant().isAfter(minBookingTime)
                ? dayStart.toInstant()
                : minBookingTime;

        // Gerar slots
        List<TimeSlot> allSlots = generateTimeSlots(
                effectiveStart,
                dayEnd.toInstant(),
                service.getDurationMinutes(),
                company.getBookingSettings().getSlotIntervalMinutes(),
                businessHours,
                zoneId
        );

        // Buscar agendamentos existentes e verificar disponibilidade
        return bookingRepository
                .findByCompanyIdAndStartTimeBetween(company.getId(), dayStart.toInstant(), dayEnd.toInstant())
                .filter(this::isActiveBooking)
                .filter(booking -> staffId == null || staffId.equals(booking.getStaffId()))
                .collectList()
                .flatMapMany(existingBookings -> checkSlotsAvailability(
                        allSlots,
                        existingBookings,
                        company.getBookingSettings().getBufferTimeMinutes(),
                        staffId,
                        zoneId
                ));
    }

    private List<TimeSlot> generateTimeSlots(Instant start, Instant end, int serviceDuration,
                                             int slotInterval, Company.BusinessHours businessHours,
                                             ZoneId zoneId) {
        List<TimeSlot> slots = new ArrayList<>();
        Instant current = start;

        while (current.plus(serviceDuration, ChronoUnit.MINUTES).isBefore(end) ||
                current.plus(serviceDuration, ChronoUnit.MINUTES).equals(end)) {

            Instant slotEnd = current.plus(serviceDuration, ChronoUnit.MINUTES);

            if (isWithinBusinessHours(current, slotEnd, businessHours, zoneId)) {
                slots.add(new TimeSlot(current, slotEnd));
            }

            current = current.plus(slotInterval, ChronoUnit.MINUTES);
        }

        return slots;
    }

    private boolean isWithinBusinessHours(Instant start, Instant end,
                                          Company.BusinessHours businessHours , ZoneId zoneId) {
        if (businessHours.getBreaks() == null || businessHours.getBreaks().isEmpty()) {
            return true;
        }

        LocalTime startTime = LocalTime.ofInstant(start, zoneId);
        LocalTime endTime = LocalTime.ofInstant(end, zoneId);

        for (Company.BusinessHours.TimeBreak timeBreak : businessHours.getBreaks()) {
            if (timesOverlap(startTime, endTime, timeBreak.getStart(), timeBreak.getEnd())) {
                return false;
            }
        }

        return true;
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !start1.isAfter(end2) && !end1.isBefore(start2);
    }

    private Flux<AvailableSlot> checkSlotsAvailability(List<TimeSlot> slots,
                                                       List<Booking> existingBookings,
                                                       int bufferTime,
                                                       String staffId,
                                                       ZoneId zoneId) {
        if (staffId != null) {
            return staffRepository.findById(staffId)
                    .flatMapMany(staff -> Flux.fromIterable(slots)
                            .map(slot -> checkSlotAvailability(slot, existingBookings, bufferTime, staff, zoneId)))
                    .switchIfEmpty(Flux.fromIterable(slots)
                            .map(slot -> new AvailableSlot(
                                    slot.getStart(),
                                    slot.getEnd(),
                                    false,
                                    "Funcionário não encontrado"
                            ))
                    );
        } else {
            return Flux.fromIterable(slots)
                    .map(slot -> checkSlotAvailability(slot, existingBookings, bufferTime, null, zoneId));
        }
    }

    private AvailableSlot checkSlotAvailability(TimeSlot slot, List<Booking> existingBookings,
                                                int bufferTime, Staff staff, ZoneId zoneId) {
        Instant slotStart = slot.getStart();
        Instant slotEnd = slot.getEnd();

        // Verificar buffer time
        Instant checkStart = slotStart.minus(bufferTime, ChronoUnit.MINUTES);
        Instant checkEnd = slotEnd.plus(bufferTime, ChronoUnit.MINUTES);

        // Verificar conflitos
        boolean hasConflict = existingBookings.stream()
                .anyMatch(booking -> bookingsOverlap(
                        checkStart, checkEnd,
                        booking.getStartTime(), booking.getEndTime()
                ));

        if (hasConflict) {
            return new AvailableSlot(slotStart, slotEnd, false, "Horário ocupado");
        }

        // Verificar disponibilidade do staff
        if (staff != null) {
            DayOfWeek dayOfWeek = slotStart.atZone(zoneId).getDayOfWeek();
            if (!staff.isWorkingAt(slotStart, dayOfWeek)) {
                return new AvailableSlot(slotStart, slotEnd, false, "Funcionário indisponível");
            }
        }

        return new AvailableSlot(slotStart, slotEnd, true);
    }

    private boolean bookingsOverlap(Instant start1, Instant end1, Instant start2, Instant end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private boolean isActiveBooking(Booking booking) {
        BookingStatus status = booking.getStatus();
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    // ==================== UTILS ====================

    private String formatDateTime(Instant instant) {
        return instant.atZone(ZoneId.of("Africa/Maputo"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Classe auxiliar para representar um slot de tempo
     */
    @Data
    @AllArgsConstructor
    private static class TimeSlot {
        private Instant start;
        private Instant end;
    }
}