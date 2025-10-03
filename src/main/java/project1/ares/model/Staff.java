package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "staffs")
public class Staff {
    @Id
    private String id;
    private String userId;  // vínculo ao User existente
    private String companyId;  // vínculo à empresa
    private List<String> services;  // Ex: HAIRDRESSER, NAIL_ARTIST, RECEPCIONIST
    private boolean active;
    private String specialization; // opcional: corte, manicure, etc.
    private String shift;          // opcional: manhã, tarde, noite

    // ========== ADICIONADO: Horários de trabalho ==========
    @Builder.Default
    private Map<DayOfWeek, WorkSchedule> workSchedule = new HashMap<>();

    // ========== ADICIONADO: Períodos de folga/férias ==========
    @Builder.Default
    private List<TimeOff> timeOffs = new ArrayList<>();

    @Builder.Default
    private Instant createdAt = Instant.now();
    @Builder.Default
    private Instant updatedAt = Instant.now();

    // ================== Nested Classes ==================

    /**
     * Horário de trabalho para um dia específico da semana
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkSchedule {
        private LocalTime startTime;    // Ex: 09:00
        private LocalTime endTime;      // Ex: 18:00
        @Builder.Default
        private boolean working = true; // Se trabalha neste dia
        @Builder.Default
        private List<TimeBreak> breaks = new ArrayList<>(); // Intervalos (almoço, etc)
    }

    /**
     * Intervalo durante o dia de trabalho (ex: almoço)
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeBreak {
        private LocalTime start;        // Ex: 12:00
        private LocalTime end;          // Ex: 13:00
        private String description;     // Ex: "Almoço"
    }

    /**
     * Período de folga, férias ou ausência
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeOff {
        private Instant startDate;
        private Instant endDate;
        @Builder.Default
        private TimeOffType type = TimeOffType.PERSONAL;
        private String reason;
        @Builder.Default
        private boolean approved = false;
        private String approvedBy;      // userId do gerente que aprovou
        private Instant approvedAt;
    }

    public enum TimeOffType {
        VACATION,       // Férias
        SICK_LEAVE,     // Licença médica
        PERSONAL,       // Motivo pessoal
        HOLIDAY,        // Feriado
        TRAINING        // Treinamento
    }

    // ================== Métodos Úteis ==================

    /**
     * Verifica se o funcionário está trabalhando em um horário específico
     */
    public boolean isWorkingAt(Instant instant, DayOfWeek dayOfWeek) {
        // 1. Verificar se está em período de folga
        if (isOnTimeOff(instant)) {
            return false;
        }

        // 2. Verificar se não está ativo
        if (!active) {
            return false;
        }

        // 3. Buscar horário do dia
        WorkSchedule schedule = workSchedule.get(dayOfWeek);
        if (schedule == null || !schedule.isWorking()) {
            return false;
        }

        // 4. Converter instant para LocalTime
        LocalTime time = LocalTime.ofInstant(instant, ZoneId.systemDefault());

        // 5. Verificar se está dentro do horário de trabalho
        boolean inWorkHours = !time.isBefore(schedule.getStartTime()) &&
                time.isBefore(schedule.getEndTime());

        if (!inWorkHours) {
            return false;
        }

        // 6. Verificar se não está em intervalo (almoço, etc)
        if (schedule.getBreaks() != null) {
            for (TimeBreak timeBreak : schedule.getBreaks()) {
                if (!time.isBefore(timeBreak.getStart()) && time.isBefore(timeBreak.getEnd())) {
                    return false; // Está em intervalo
                }
            }
        }

        return true;
    }

    /**
     * Verifica se está em período de folga aprovado
     */
    public boolean isOnTimeOff(Instant instant) {
        if (timeOffs == null || timeOffs.isEmpty()) {
            return false;
        }

        return timeOffs.stream()
                .filter(TimeOff::isApproved)
                .anyMatch(timeOff ->
                        !instant.isBefore(timeOff.getStartDate()) &&
                                instant.isBefore(timeOff.getEndDate())
                );
    }

    /**
     * Retorna o horário de trabalho para um dia específico
     */
    public WorkSchedule getScheduleForDay(DayOfWeek dayOfWeek) {
        return workSchedule.getOrDefault(dayOfWeek, createDefaultSchedule(dayOfWeek));
    }

    /**
     * Cria horário padrão se não existir
     */
    private WorkSchedule createDefaultSchedule(DayOfWeek dayOfWeek) {
        WorkSchedule schedule = new WorkSchedule();

        if (dayOfWeek == DayOfWeek.SUNDAY) {
            schedule.setWorking(false);
        } else {
            schedule.setStartTime(LocalTime.of(8, 0));
            schedule.setEndTime(LocalTime.of(17, 0));
            schedule.setWorking(true);
            schedule.setBreaks(new ArrayList<>());
        }

        return schedule;
    }

    /**
     * Verifica se pode prestar um serviço específico
     */
    public boolean canProvideService(String serviceId) {
        return services != null && services.contains(serviceId);
    }
}
