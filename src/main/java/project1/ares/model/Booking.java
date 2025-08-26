package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;

    private String clientId;
    private String serviceId;
    private String companyId;
    private String staffId; // opcional: funcionário designado

    private Instant startTime;
    private Instant endTime;

    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;
}
