package project1.ares.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import project1.ares.model.CancellationReason;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelBookingRequest {
    @NotNull(message = "Razão do cancelamento é obrigatória")
    private CancellationReason reason;

    private String additionalNotes; // Observações adicionais opcionais
}