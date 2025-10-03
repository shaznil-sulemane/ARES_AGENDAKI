package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import project1.ares.dto.create.BookingCREATE;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;

    // ============ REFERÊNCIAS (sempre manter) ============
    private String clientId;
    private String serviceId;
    private String companyId;
    private String staffId; // opcional: funcionário designado

    // ============ SNAPSHOT DO CLIENTE (momento do agendamento) ============
    private String clientName;     // User.fullName quando agendou
    private String clientEmail;    // User.email quando agendou
    private String clientPhone;    // User.phoneNumber quando agendou

    // ============ SNAPSHOT DO SERVIÇO (momento do agendamento) ============
    private String serviceName;    // Service.name quando agendou
    private BigDecimal servicePrice; // Service.price quando agendou
    private int serviceDurationMinutes; // Service.durationMinutes quando agendou
    private String serviceCategory; // Service.category quando agendou

    // ============ INFORMAÇÕES OPERACIONAIS ============
    private Instant startTime;
    private Instant endTime;
    private String timeZone; // fuso horário do agendamento

    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    // ============ INFORMAÇÕES FINANCEIRAS ============
    private String currency;        // moeda usada quando agendou
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // ============ AUDITORIA E CONTROLE ============
    @Builder.Default
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
    private String createdBy; // quem criou (cliente, staff, admin)

    @Version
    private Long version; // controle de concorrência otimista

    // ============ GESTÃO DE CANCELAMENTOS ============
    private Instant cancelledAt;
    private String cancelledBy;
    private String cancellationReason;
    private Instant confirmedAt;

    // ============ CONTROLE DE NOTIFICAÇÕES ============
    @Builder.Default
    private Boolean emailConfirmationSent = false;
    @Builder.Default
    private Boolean emailReminderSent = false;
    @Builder.Default
    private Boolean smsReminderSent = false;

    // ============ OBSERVAÇÕES E RECURSOS ============
    private String notes; // observações internas
    private String clientNotes; // observações do cliente
    private List<String> resourceIds; // equipamentos, salas específicas necessárias

    // ============ PARA FUTURO: AGENDAMENTOS RECORRENTES ============
    private String recurrenceId;
    private RecurrencePattern recurrencePattern;

    // ============ MÉTODOS DE CONVENIÊNCIA ============
    public boolean isActive() {
        return status == BookingStatus.PENDING || status == BookingStatus.CONFIRMED;
    }

    public boolean canBeCancelled() {
        return isActive() && startTime.isAfter(Instant.now());
    }

    public boolean isInPast() {
        return endTime.isBefore(Instant.now());
    }

    public boolean isPending() {
        return status == BookingStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    // ============ MÉTODOS DE LIFECYCLE ============
    /**@PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
    */

    // ============ FACTORY METHOD ============
    public static Booking createFromEntities(User client, project1.ares.model.Service service,
                                             BookingCREATE request, String createdBy) {
        Instant startTime = request.getStartTime();
        Instant endTime = startTime.plus(service.getDurationMinutes(), ChronoUnit.MINUTES);

        return Booking.builder()
                // Referências
                .clientId(client.getId())
                .serviceId(service.getId())
                .companyId(request.getCompanyId())
                .staffId(request.getStaffId())

                // Snapshot do cliente
                .clientName(client.getFullName())
                .clientEmail(client.getEmail())
                .clientPhone(client.getPhoneNumber())

                // Snapshot do serviço
                .serviceName(service.getName())
                .servicePrice(service.getPrice())
                .serviceDurationMinutes(service.getDurationMinutes())
                .serviceCategory(service.getCategory())

                // Dados operacionais
                .startTime(startTime)
                .endTime(endTime)
                .timeZone("Africa/Maputo") // ou baseado no cliente
                .currency(client.getPreferences().getCurrency())
                .createdBy(createdBy)
                .clientNotes(request.getClientNotes())

                .build();
    }

    private static String determineClientTimezone(User client) {
        // Lógica básica baseada na moeda
        String currency = client.getPreferences().getCurrency();
        if ("MZN".equals(currency)) return "Africa/Maputo";
        if ("ZAR".equals(currency)) return "Africa/Johannesburg";
        if ("USD".equals(currency)) return "America/New_York";
        if ("EUR".equals(currency)) return "Europe/London";
        if ("BRL".equals(currency)) return "America/Sao_Paulo";

        // Verificação rápida do telefone
        String phone = client.getPhoneNumber();
        if (phone != null) {
            if (phone.contains("+258")) return "Africa/Maputo";
            if (phone.contains("+27")) return "Africa/Johannesburg";
            if (phone.contains("+55")) return "America/Sao_Paulo";
        }

        return "Africa/Maputo"; // padrão
    }
}
