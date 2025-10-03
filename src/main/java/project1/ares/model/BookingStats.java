package project1.ares.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class BookingStats {
    private List<BookingStatusCount> statusCounts;
    private long totalBookings;
    private long pendingCount;
    private long confirmedCount;
    private long completedCount;
    private long canceledCount;
    private BigDecimal totalRevenue;

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

    /**
     * Calcula a taxa de ocupação (confirmados + completados / total)
     */
    public double getOccupancyRate() {
        if (totalBookings == 0) return 0.0;
        return ((double) (confirmedCount + completedCount) / totalBookings) * 100;
    }

    /**
     * Calcula a taxa de cancelamento
     */
    public double getCancellationRate() {
        if (totalBookings == 0) return 0.0;
        return ((double) canceledCount / totalBookings) * 100;
    }

    /**
     * Calcula o ticket médio
     */
    public BigDecimal getAverageTicket() {
        if (completedCount == 0) return BigDecimal.ZERO;
        return totalRevenue.divide(BigDecimal.valueOf(completedCount), 2, BigDecimal.ROUND_HALF_UP);
    }
}
