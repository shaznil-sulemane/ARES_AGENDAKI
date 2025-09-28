package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {
    @Id
    private String id;

    private String bookingId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus paymentStatus;

    private boolean success;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
