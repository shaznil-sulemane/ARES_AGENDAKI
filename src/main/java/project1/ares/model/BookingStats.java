package project1.ares.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BookingStats {
    private List<BookingStatusCount> statusCounts;

    public long getTotalBookings() {
        return statusCounts.stream().mapToLong(BookingStatusCount::getCount).sum();
    }

    public long getPendingCount() {
        return statusCounts.stream()
                .filter(sc -> sc.getStatus() == BookingStatus.PENDING)
                .mapToLong(BookingStatusCount::getCount)
                .sum();
    }

    public long getConfirmedCount() {
        return statusCounts.stream()
                .filter(sc -> sc.getStatus() == BookingStatus.CONFIRMED)
                .mapToLong(BookingStatusCount::getCount)
                .sum();
    }
}
