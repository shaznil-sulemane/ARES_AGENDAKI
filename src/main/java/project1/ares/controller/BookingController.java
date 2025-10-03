package project1.ares.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import project1.ares.config.ApiResponse;
import project1.ares.config.BookingException;
import project1.ares.config.CustomUserDetails;
import project1.ares.dto.create.BookingCREATE;
import project1.ares.dto.request.CancelBookingRequest;
import project1.ares.model.AvailableSlot;
import project1.ares.model.Booking;
import project1.ares.model.BookingStats;
import project1.ares.model.BookingStatus;
import org.springframework.web.bind.annotation.*;
import project1.ares.service.BookingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // ==================== CRIAR AGENDAMENTO ====================

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<Booking>>> createBooking(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody BookingCREATE bookingCreate
    ) {
        // Garantir que o cliente está criando agendamento para si mesmo
        // (a menos que seja ADMIN ou MANAGER)
        if (!principal.getId().equals(bookingCreate.getClientId())
                && !principal.hasRole("ADMIN")
                && !principal.hasRole("MANAGER")) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Você não tem permissão para criar agendamento para outro usuário"))
            );
        }

        // Definir quem criou o agendamento
        bookingCreate.setCreatedBy(principal.getId());

        return bookingService.createBooking(bookingCreate)
                .map(booking -> ResponseEntity
                        .status(HttpStatus.CREATED)
                        .body(ApiResponse.success(booking, "Agendamento criado com sucesso"))
                )
                .onErrorResume(BookingException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(ex.getMessage()))
                        )
                )
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(ex.getMessage()))
                        )
                )
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("Erro ao criar agendamento: " + ex.getMessage()))
                        )
                );
    }

    // ==================== BUSCAR AGENDAMENTOS DO CLIENTE ====================

    @GetMapping("/client/{clientId}")
    @PreAuthorize("#clientId == #principal.id or hasRole('ADMIN') or hasRole('MANAGER')")
    public Flux<Booking> getClientBookings(
            @PathVariable String clientId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) BookingStatus status
    ) {
        if (status != null) {
            return bookingService.getClientBookingsByStatus(clientId, status);
        }
        return bookingService.getAllClientBookings(clientId);
    }

    @GetMapping("/client/{clientId}/active")
    @PreAuthorize("#clientId == #principal.id or hasRole('ADMIN') or hasRole('MANAGER')")
    public Mono<ResponseEntity<ApiResponse<Flux<Booking>>>> getActiveClientBookings(
            @PathVariable String clientId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return bookingService.getClientBookings(clientId)
                .map(bookings -> ResponseEntity.ok(
                        ApiResponse.success(bookings, "Agendamentos ativos recuperados com sucesso")
                ))
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("Erro ao buscar agendamentos: " + ex.getMessage()))
                        )
                );
    }

    // ==================== BUSCAR AGENDAMENTO POR ID ====================

    @GetMapping("/{bookingId}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<Booking>>> getBookingById(
            @PathVariable String bookingId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return bookingService.getBookingById(bookingId)
                .flatMap(booking -> {
                    // Verificar permissões: apenas o cliente, staff ou admin pode ver
                    if (!booking.getClientId().equals(principal.getId())
                            && !principal.hasRole("ADMIN")
                            && !principal.hasRole("MANAGER")) {
                        return Mono.just(ResponseEntity
                                .status(HttpStatus.FORBIDDEN)
                                .body(ApiResponse.<Booking>error("Você não tem permissão para visualizar este agendamento"))
                        );
                    }

                    return Mono.just(ResponseEntity.ok(
                            ApiResponse.success(booking, "Agendamento encontrado")
                    ));
                })
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Agendamento não encontrado"))
                ));
    }

    // ==================== CANCELAR AGENDAMENTO ====================

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<Booking>>> cancelBooking(
            @PathVariable String bookingId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody(required = false) CancelBookingRequest request
    ) {
        String reason = (request != null && request.getReason() != null)
                ? String.valueOf(request.getReason())
                : "Cancelado pelo usuário";

        return bookingService.cancelBooking(bookingId, principal.getId(), reason)
                .map(booking -> ResponseEntity.ok(
                        ApiResponse.success(booking, "Agendamento cancelado com sucesso")
                ))
                .onErrorResume(BookingException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(ApiResponse.error(ex.getMessage()))
                        )
                )
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ex.getMessage()))
                        )
                );
    }

    // ==================== CONFIRMAR AGENDAMENTO (ADMIN/MANAGER) ====================

    @PutMapping("/{bookingId}/confirm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Mono<ResponseEntity<ApiResponse<Booking>>> confirmBooking(
            @PathVariable String bookingId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return bookingService.confirmBooking(bookingId, principal.getId())
                .map(booking -> ResponseEntity.ok(
                        ApiResponse.success(booking, "Agendamento confirmado com sucesso")
                ))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error(ex.getMessage()))
                        )
                );
    }

    // ==================== BUSCAR AGENDAMENTOS POR EMPRESA ====================

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Flux<Booking> getCompanyBookings(
            @PathVariable String companyId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate
    ) {
        if (startDate != null && endDate != null) {
            return bookingService.getBookingsByDateRange(companyId, startDate, endDate);
        }
        return bookingService.getCompanyBookings(companyId);
    }

    // ==================== BUSCAR AGENDAMENTOS POR SERVIÇO ====================

    @GetMapping("/service/{serviceName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Mono<ResponseEntity<ApiResponse<java.util.List<Booking>>>> getBookingsByServiceName(
            @PathVariable String serviceName
    ) {
        return bookingService.getBookingsByServiceName(serviceName)
                .map(bookings -> ResponseEntity.ok(
                        ApiResponse.success(bookings, "Agendamentos encontrados")
                ));
    }

    // ==================== BUSCAR AGENDAMENTOS POR FAIXA DE PREÇO ====================

    @GetMapping("/price-range")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Mono<ResponseEntity<ApiResponse<java.util.List<Booking>>>> getBookingsInPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice
    ) {
        return bookingService.getBookingsInPriceRange(minPrice, maxPrice)
                .map(bookings -> ResponseEntity.ok(
                        ApiResponse.success(bookings, "Agendamentos encontrados")
                ));
    }

    // ==================== ESTATÍSTICAS (ADMIN/MANAGER) ====================

    @GetMapping("/stats/{companyId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public Mono<ResponseEntity<ApiResponse<BookingStats>>> getBookingStats(
            @PathVariable String companyId
    ) {
        return bookingService.getBookingStats(companyId)
                .map(stats -> ResponseEntity.ok(
                        ApiResponse.success(stats, "Estatísticas recuperadas com sucesso")
                ))
                .onErrorResume(ex ->
                        Mono.just(ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.error("Erro ao buscar estatísticas: " + ex.getMessage()))
                        )
                );
    }

    // ==================== ATUALIZAR AGENDAMENTOS EXPIRADOS ====================

    @PostMapping("/update-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<ApiResponse<Long>>> updateExpiredBookings() {
        return bookingService.updateExpiredBookingsToCompleted()
                .map(count -> ResponseEntity.ok(
                        ApiResponse.success(count, count + " agendamentos atualizados para COMPLETED")
                ));
    }

    // ==================== BUSCAR HORÁRIOS DISPONÍVEIS - ENDPOINTS APRIMORADOS ====================

    /**
     * ATUALIZADO: Buscar todos os slots (disponíveis e ocupados)
     * GET /bookings/available-slots?companyId=123&serviceId=456&date=2025-10-15
     */
    @GetMapping("/available-slots")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<java.util.List<AvailableSlot>>>> getAvailableSlots(
            @RequestParam String companyId,
            @RequestParam(required = false) String staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam String serviceId
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            Instant instant = localDate.atStartOfDay(ZoneId.of("Africa/Maputo")).toInstant();

            return bookingService.getAvailableSlots(companyId, staffId, instant, serviceId)
                    .collectList()
                    .map(slots -> ResponseEntity.ok(
                            ApiResponse.success(slots, "Slots recuperados com sucesso")
                    ))
                    .onErrorResume(IllegalArgumentException.class, ex ->
                            Mono.just(ResponseEntity
                                    .status(HttpStatus.BAD_REQUEST)
                                    .body(ApiResponse.error(ex.getMessage()))
                            )
                    );
        } catch (Exception e) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Formato de data inválido. Use YYYY-MM-DD"))
            );
        }
    }

    /**
     * NOVO: Buscar apenas slots disponíveis
     * GET /bookings/available-slots/only-available?companyId=123&serviceId=456&date=2025-10-15
     */
    @GetMapping("/available-slots/only-available")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<java.util.List<AvailableSlot>>>> getOnlyAvailableSlots(
            @RequestParam String companyId,
            @RequestParam(required = false) String staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam String serviceId
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            Instant instant = localDate.atStartOfDay(ZoneId.of("Africa/Maputo")).toInstant();

            return bookingService.getAvailableSlots(companyId, staffId, instant, serviceId)
                    .filter(AvailableSlot::isAvailable)
                    .collectList()
                    .map(slots -> ResponseEntity.ok(
                            ApiResponse.success(slots, "Slots disponíveis recuperados com sucesso")
                    ))
                    .onErrorResume(ex ->
                            Mono.just(ResponseEntity
                                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(ApiResponse.error("Erro ao buscar slots: " + ex.getMessage()))
                            )
                    );
        } catch (Exception e) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Formato de data inválido. Use YYYY-MM-DD"))
            );
        }
    }

    /**
     * NOVO: Contar slots disponíveis
     * GET /bookings/available-slots/count?companyId=123&serviceId=456&date=2025-10-15
     */
    @GetMapping("/available-slots/count")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> countAvailableSlots(
            @RequestParam String companyId,
            @RequestParam(required = false) String staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam String serviceId
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            Instant instant = localDate.atStartOfDay(ZoneId.of("Africa/Maputo")).toInstant();

            Mono<Long> totalSlots = bookingService.getAvailableSlots(companyId, staffId, instant, serviceId)
                    .count();

            Mono<Long> availableSlots = bookingService.getAvailableSlots(companyId, staffId, instant, serviceId)
                    .filter(AvailableSlot::isAvailable)
                    .count();

            return Mono.zip(totalSlots, availableSlots)
                    .map(tuple -> {
                        long total = tuple.getT1();
                        long available = tuple.getT2();
                        long occupied = total - available;

                        Map<String, Object> result = Map.of(
                                "date", date,
                                "totalSlots", total,
                                "availableSlots", available,
                                "occupiedSlots", occupied,
                                "occupancyRate", total > 0
                                        ? String.format("%.1f%%", occupied * 100.0 / total)
                                        : "0%"
                        );

                        return ResponseEntity.ok(
                                ApiResponse.success(result, "Contagem de slots realizada com sucesso")
                        );
                    });
        } catch (Exception e) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Formato de data inválido. Use YYYY-MM-DD"))
            );
        }
    }

    /**
     * NOVO: Primeiro slot disponível
     * GET /bookings/available-slots/first-available?companyId=123&serviceId=456&date=2025-10-15
     */
    @GetMapping("/available-slots/first-available")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<AvailableSlot>>> getFirstAvailableSlot(
            @RequestParam String companyId,
            @RequestParam(required = false) String staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam String serviceId
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            Instant instant = localDate.atStartOfDay(ZoneId.of("Africa/Maputo")).toInstant();

            return bookingService.getAvailableSlots(companyId, staffId, instant, serviceId)
                    .filter(AvailableSlot::isAvailable)
                    .next()
                    .map(slot -> ResponseEntity.ok(
                            ApiResponse.success(slot, "Primeiro slot disponível encontrado")
                    ))
                    .switchIfEmpty(Mono.just(ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(ApiResponse.error("Nenhum slot disponível encontrado para a data"))
                    ));
        } catch (Exception e) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Formato de data inválido. Use YYYY-MM-DD"))
            );
        }
    }

    /**
     * NOVO: Resumo completo de disponibilidade
     * GET /bookings/available-slots/summary?companyId=123&serviceId=456&date=2025-10-15
     */
    @GetMapping("/available-slots/summary")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getAvailabilitySummary(
            @RequestParam String companyId,
            @RequestParam(required = false) String staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String date,
            @RequestParam String serviceId
    ) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            Instant instant = localDate.atStartOfDay(ZoneId.of("Africa/Maputo")).toInstant();

            return bookingService.getAvailableSlots(companyId, staffId, instant, serviceId)
                    .collectList()
                    .map(slots -> {
                        long available = slots.stream().filter(AvailableSlot::isAvailable).count();
                        long occupied = slots.size() - available;

                        AvailableSlot firstAvailable = slots.stream()
                                .filter(AvailableSlot::isAvailable)
                                .findFirst()
                                .orElse(null);

                        AvailableSlot lastAvailable = slots.stream()
                                .filter(AvailableSlot::isAvailable)
                                .reduce((first, second) -> second)
                                .orElse(null);

                        Map<String, Object> summary = Map.ofEntries(
                                Map.entry("date", date),
                                Map.entry("companyId", companyId),
                                Map.entry("serviceId", serviceId),
                                Map.entry("staffId", staffId != null ? staffId : "all"),
                                Map.entry("totalSlots", slots.size()),
                                Map.entry("availableSlots", available),
                                Map.entry("occupiedSlots", occupied),
                                Map.entry("hasAvailability", available > 0),
                                Map.entry("firstAvailableTime", firstAvailable != null ? firstAvailable.getStartTime() : null),
                                Map.entry("lastAvailableTime", lastAvailable != null ? lastAvailable.getStartTime() : null),
                                Map.entry("occupancyRate", slots.size() > 0
                                        ? String.format("%.1f%%", occupied * 100.0 / slots.size())
                                        : "0%")
                        );


                        return ResponseEntity.ok(
                                ApiResponse.success(summary, "Resumo de disponibilidade gerado com sucesso")
                        );
                    });
        } catch (Exception e) {
            return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Formato de data inválido. Use YYYY-MM-DD"))
            );
        }
    }
}