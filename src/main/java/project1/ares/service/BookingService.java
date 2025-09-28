package project1.ares.service;

import org.springframework.stereotype.Service;
import project1.ares.config.BookingException;
import project1.ares.dto.create.BookingCREATE;
import project1.ares.model.Booking;
import project1.ares.model.BookingStatus;
import project1.ares.model.User;
import project1.ares.repository.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final CompanyRepository companyRepository;
    private final StaffRepository staffRepository;
    private final project1.ares.service.EmailService emailService;

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

    public Mono<Booking> createBooking(BookingCREATE bookingCreate) {
        return validateAndCreateBooking(bookingCreate)
                .flatMap(this::saveBooking)
                .flatMap(this::sendConfirmationEmail)
                .doOnError(error -> log.error("Erro ao criar agendamento: {}", error.getMessage()));
    }

    private Mono<Booking> validateAndCreateBooking(BookingCREATE bookingCreate) {
        // Buscar cliente e serviço em paralelo de forma reativa
        Mono<User> clientMono = userRepository.findById(bookingCreate.getClientId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Cliente não encontrado")));

        Mono<project1.ares.model.Service> serviceMono = serviceRepository.findById(bookingCreate.getServiceId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Serviço não encontrado")));

        return Mono.zip(clientMono, serviceMono)
                .flatMap(tuple -> {
                    User client = tuple.getT1();
                    project1.ares.model.Service service = tuple.getT2();

                    // Calcular horários
                    Instant startTime = bookingCreate.getStartTime();
                    Instant endTime = startTime.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);

                    // Verificar disponibilidade (versão completamente reativa)
                    return checkTimeSlotAvailability(bookingCreate.getCompanyId(),
                            bookingCreate.getStaffId(),
                            startTime,
                            endTime)
                            .then(Mono.fromCallable(() ->
                                    Booking.createFromEntities(client, service, bookingCreate, bookingCreate.getCreatedBy())
                            ));
                });
    }


    private Mono<Void> checkTimeSlotAvailability(String companyId,
                                                 String staffId,
                                                 Instant startTime,
                                                 Instant endTime) {
        Mono<Void> companyCheck = checkCompanyAvailability(companyId, startTime, endTime);

        Mono<Void> staffCheck = (staffId != null)
                ? checkStaffAvailability(staffId, startTime, endTime)
                : Mono.empty();

        Mono<Void> bookingCheck = checkBookingConflicts(companyId, staffId, startTime, endTime);

        return Mono.when(companyCheck, staffCheck, bookingCheck);
    }


    private Mono<Void> checkCompanyAvailability(String companyId,
                                                Instant startTime,
                                                Instant endTime) {
        return companyRepository.isAvailable(companyId, startTime, endTime)
                .flatMap(available -> available
                        ? Mono.<Void>empty()
                        : Mono.error(new IllegalArgumentException("Empresa indisponível nesse horário"))
                );
    }

    private Mono<Void> checkStaffAvailability(String staffId,
                                              Instant startTime,
                                              Instant endTime) {
        return staffRepository.isAvailable(staffId, startTime, endTime)
                .flatMap(available -> available
                        ? Mono.<Void>empty()
                        : Mono.error(new IllegalArgumentException("Funcionário indisponível nesse horário"))
                );
    }

    private Mono<Void> checkBookingConflicts(String companyId,
                                             String staffId,
                                             Instant startTime,
                                             Instant endTime) {
        return bookingRepository.findConflictingBookings(companyId, staffId, startTime, endTime)
                .collectList() // junta em uma lista
                .flatMap(conflictingBookings -> {
                    if (!conflictingBookings.isEmpty()) {
                        return Mono.error(new BookingException(
                                "Horário indisponível - Já existe agendamento neste período"));
                    }
                    return Mono.empty(); // se não houver conflito, completa normalmente
                });
    }


    private Mono<Booking> saveBooking(Booking booking) {
        return bookingRepository.save(booking)
                .doOnSuccess(saved -> log.info("Agendamento criado com sucesso: {}", saved.getId()));
    }


    private Mono<Booking> sendConfirmationEmail(Booking booking) {
        // Agora os dados já estão na entidade Booking - sem necessidade de consultas!
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
                        1, // template number para confirmação
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
                    return Mono.just(booking); // Continua mesmo se email falhar
                });
    }

    private String determineTimeZone(User client) {
        // Lógica para determinar timezone baseado no cliente
        return "Africa/Maputo"; // Para Moçambique
    }

    private String formatDateTime(Instant instant) {
        return instant.atZone(ZoneId.of("Africa/Maputo"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    // Métodos adicionais úteis
    public Mono<List<Booking>> getClientBookings(String clientId) {
        return Mono.fromCallable(() ->
                bookingRepository.findByClientIdAndStatusIn(
                        clientId,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                )
        );
    }

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


    // Métodos adicionais para aproveitar os dados desnormalizados
    public Mono<List<Booking>> getBookingsByServiceName(String serviceName) {
        return Mono.fromCallable(() ->
                bookingRepository.findByServiceNameAndStatusIn(serviceName,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED))
        );
    }

    public Mono<List<Booking>> getBookingsInPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return Mono.fromCallable(() ->
                bookingRepository.findByServicePriceBetween(minPrice, maxPrice)
        );
    }
}

