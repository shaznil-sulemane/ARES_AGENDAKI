package project1.ares.dto.create;

import lombok.Data;

import java.time.Instant;

@Data
public class BookingCREATE {
    private String clientId;
    private String serviceId;
    private String companyId;
    private String staffId;
    private Instant startTime;
}

