package project1.ares.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO para representar um slot de horário disponível
 */
@Data
@NoArgsConstructor
public class AvailableSlot {
    private Instant startTime;
    private Instant endTime;
    private boolean available;

    // Informações adicionais úteis
    private String reason; // Motivo da indisponibilidade (se aplicável)

    public AvailableSlot(Instant startTime, Instant endTime, boolean available) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
        this.reason = available ? null : "Horário ocupado";
    }

    public AvailableSlot(Instant startTime, Instant endTime, boolean available, String reason) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.available = available;
        this.reason = reason;
    }
}
