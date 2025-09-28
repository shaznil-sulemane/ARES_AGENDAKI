package project1.ares.dto.create;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingCREATE {
    private String clientId;
    private String serviceId;
    private String companyId;
    private String staffId; // opcional
    private Instant startTime;
    private String clientNotes; // observações do cliente
    private String createdBy;
}
