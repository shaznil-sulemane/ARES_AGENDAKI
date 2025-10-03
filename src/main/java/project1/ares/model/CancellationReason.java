package project1.ares.model;

import lombok.Getter;

@Getter
public enum CancellationReason {
    // Razões do cliente
    PERSONAL_EMERGENCY("Emergência pessoal", "Cliente teve uma emergência pessoal"),
    SCHEDULING_CONFLICT("Conflito de agenda", "Cliente tem outro compromisso no mesmo horário"),
    ILLNESS("Doença", "Cliente está doente ou não se sente bem"),
    FINANCIAL_REASONS("Motivos financeiros", "Cliente não pode arcar com o custo neste momento"),
    CHANGE_OF_PLANS("Mudança de planos", "Cliente mudou seus planos"),
    TRAVEL("Viagem", "Cliente estará viajando"),
    WEATHER("Condições climáticas", "Condições climáticas desfavoráveis"),
    TRANSPORTATION("Problema de transporte", "Cliente teve problema de transporte"),
    FOUND_ALTERNATIVE("Encontrou alternativa", "Cliente encontrou outro estabelecimento"),
    NO_LONGER_NEEDED("Não precisa mais", "Cliente não precisa mais do serviço"),

    // Razões do estabelecimento
    STAFF_UNAVAILABLE("Funcionário indisponível", "Funcionário designado não está disponível"),
    EQUIPMENT_MALFUNCTION("Problema com equipamento", "Equipamento necessário está com defeito"),
    ESTABLISHMENT_CLOSED("Estabelecimento fechado", "Estabelecimento fechará neste dia"),
    OVERBOOKING("Overbooking", "Erro de agendamento duplo"),
    SERVICE_DISCONTINUED("Serviço descontinuado", "Serviço não é mais oferecido"),

    // Razões administrativas
    DUPLICATE_BOOKING("Agendamento duplicado", "Cliente fez agendamento duplicado"),
    PAYMENT_FAILED("Falha no pagamento", "Pagamento não foi processado com sucesso"),
    FRAUDULENT("Suspeita de fraude", "Agendamento suspeito de fraude"),
    NO_SHOW_HISTORY("Histórico de faltas", "Cliente tem histórico de não comparecer"),

    // Outros
    TEST_BOOKING("Agendamento de teste", "Agendamento criado para teste do sistema"),
    OTHER("Outro motivo", "Outro motivo não especificado");

    private final String displayName;
    private final String description;

    CancellationReason(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Retorna as razões disponíveis para clientes
     */
    public static CancellationReason[] getClientReasons() {
        return new CancellationReason[]{
                PERSONAL_EMERGENCY,
                SCHEDULING_CONFLICT,
                ILLNESS,
                FINANCIAL_REASONS,
                CHANGE_OF_PLANS,
                TRAVEL,
                WEATHER,
                TRANSPORTATION,
                FOUND_ALTERNATIVE,
                NO_LONGER_NEEDED,
                OTHER
        };
    }

    /**
     * Retorna as razões disponíveis para o estabelecimento
     */
    public static CancellationReason[] getEstablishmentReasons() {
        return new CancellationReason[]{
                STAFF_UNAVAILABLE,
                EQUIPMENT_MALFUNCTION,
                ESTABLISHMENT_CLOSED,
                OVERBOOKING,
                SERVICE_DISCONTINUED,
                OTHER
        };
    }

    /**
     * Retorna as razões administrativas
     */
    public static CancellationReason[] getAdministrativeReasons() {
        return new CancellationReason[]{
                DUPLICATE_BOOKING,
                PAYMENT_FAILED,
                FRAUDULENT,
                NO_SHOW_HISTORY,
                TEST_BOOKING,
                OTHER
        };
    }

    /**
     * Verifica se a razão é válida para um cliente comum
     */
    public boolean isClientReason() {
        for (CancellationReason reason : getClientReasons()) {
            if (reason == this) return true;
        }
        return false;
    }

    /**
     * Verifica se a razão requer permissão especial (admin/manager)
     */
    public boolean requiresAdminPermission() {
        return this == FRAUDULENT ||
                this == NO_SHOW_HISTORY ||
                this == TEST_BOOKING;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
