package project1.ares.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import project1.ares.config.CustomUserDetails;
import project1.ares.dto.create.BookingCREATE;
import project1.ares.model.Booking;
import project1.ares.model.BookingStatus;
import project1.ares.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @GetMapping("/{clientId}")
    @PreAuthorize("#clientId == #principal.id or hasRole('ADMIN')")
    public Flux<Booking> getUserBookings(
            @PathVariable String clientId,
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return bookingRepository.findByClientId(clientId);
    }


    @PostMapping
    @PreAuthorize("(hasRole('MANAGER') and #req.clientId == #principal.id) or hasRole('ADMIN')")
    public Mono<Booking> createBooking(@AuthenticationPrincipal CustomUserDetails principal, @RequestBody BookingCREATE req) {
        Booking booking = new Booking();
        booking.setClientId(req.getClientId());

        return bookingRepository.save(booking);
    }
}
