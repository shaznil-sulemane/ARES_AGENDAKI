package project1.ares.model;

import lombok.AllArgsConstructor;
import lombok.Data;

// Classes auxiliares para estatísticas
@Data
@AllArgsConstructor
class BookingStatusCount {
    private BookingStatus status;
    private Long count;
}
