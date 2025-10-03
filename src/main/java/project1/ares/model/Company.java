package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Document(collection = "companies")
public class Company {

    @Id
    private String id;
    private String name = "";
    private String email = "";
    private String phone = "";
    private String address = "";
    private Map<String, Object> metadata = new HashMap<>();
    private CompanyType type = CompanyType.OTHER;
    private boolean active = false;
    private GeoLocation location = new GeoLocation(0.0, 0.0);
    private String owner = "";
    private String ownerName = "";
    private String timeZone = "Africa/Maputo"; // Fuso horário da empresa

    private Plan plan = new Plan();
    private List<String> managers = new ArrayList<>();
    private List<String> staff = new ArrayList<>();

    // ADICIONADO: Horários de funcionamento
    private Map<DayOfWeek, BusinessHours> businessHours = new HashMap<>();

    // Configurações de agendamento
    private BookingSettings bookingSettings = new BookingSettings();

    @CreatedDate
    private Instant createdAt = Instant.now();
    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    private LocalDate planStartDate;
    private LocalDate planEndDate;

    @Data
    public static class Plan {
        private String name = "";
        private BigDecimal price = BigDecimal.ZERO;
        private String title = "";
        private project1.ares.model.Plan.Duration duration = project1.ares.model.Plan.Duration.MENSAL;
        private String description = "";
        private boolean active = false;
        private List<String> features = new ArrayList<>();
        private String badge;
        private String urlToPay;
        private int position = 0;
    }

    public boolean isOpenAt(Instant instant) {
        ZoneId zoneId = ZoneId.of(this.timeZone);
        LocalDate localDate = instant.atZone(zoneId).toLocalDate();
        LocalTime localTime = instant.atZone(zoneId).toLocalTime();
        DayOfWeek dayOfWeek = localDate.getDayOfWeek();

        BusinessHours hours = getBusinessHoursForDay(dayOfWeek);

        if (hours.isClosed()) {
            return false;
        }

        return hours.isOpen(localTime);
    }

    /**
     * Configurações específicas para agendamentos
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookingSettings {
        private int slotIntervalMinutes = 30; // Intervalo padrão entre slots
        private int bufferTimeMinutes = 0; // Tempo de buffer entre agendamentos
        private int maxAdvanceBookingDays = 30; // Máximo de dias para agendar antecipadamente
        private int minAdvanceBookingHours = 2; // Mínimo de horas de antecedência
        private boolean allowSameDayBooking = true;
        private boolean requireConfirmation = false;
    }

    /**
     * Retorna os horários de funcionamento para um dia específico
     */
    public BusinessHours getBusinessHoursForDay(DayOfWeek dayOfWeek) {
        return businessHours.getOrDefault(dayOfWeek, createDefaultBusinessHours(dayOfWeek));
    }

    /**
     * Cria horário de funcionamento padrão se não existir
     */
    private BusinessHours createDefaultBusinessHours(DayOfWeek dayOfWeek) {
        BusinessHours hours = new BusinessHours();
        hours.setDayOfWeek(dayOfWeek);

        // Domingo fechado por padrão
        if (dayOfWeek == DayOfWeek.SUNDAY) {
            hours.setClosed(true);
        } else {
            hours.setOpenTime(LocalTime.of(8, 0));
            hours.setCloseTime(LocalTime.of(18, 0));
            hours.setClosed(false);
        }

        return hours;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusinessHours {
        private DayOfWeek dayOfWeek;
        private LocalTime openTime;
        private LocalTime closeTime;
        private boolean closed; // Se a empresa está fechada neste dia
        private List<TimeBreak> breaks; // Intervalos (ex: horário de almoço)

        /**
         * Verifica se a empresa está aberta em um horário específico
         */
        public boolean isOpen(LocalTime time) {
            if (closed) {
                return false;
            }

            boolean inWorkingHours = !time.isBefore(openTime) && time.isBefore(closeTime);

            if (inWorkingHours && breaks != null) {
                // Verificar se está em algum intervalo
                for (TimeBreak timeBreak : breaks) {
                    if (!time.isBefore(timeBreak.getStart()) && time.isBefore(timeBreak.getEnd())) {
                        return false;
                    }
                }
            }

            return inWorkingHours;
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TimeBreak {
            private LocalTime start;
            private LocalTime end;
            private String description; // ex: "Almoço", "Pausa"
        }
    }
}

