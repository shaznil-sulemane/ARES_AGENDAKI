package project1.ares.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {

    @Id
    private String id;

    // Identidade básica
    private String fullName = "";
    private String username = "";
    private String email = "";
    private String phoneNumber = "";

    // Segurança & autenticação
    private Security security = new Security();

    // Papel & permissões
    private Role role = Role.USER;

    // Plano de assinatura
    private SubscriptionPlan subscription = new SubscriptionPlan();

    // Preferências do usuário
    private Preferences preferences = new Preferences();

    // Auditoria
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Status status = Status.ACTIVE; // substitui enabled

    // ================== Enums ==================
    public enum Role {
        USER, ADMIN, MANAGER, STAFF
    }

    public enum PlanType {
        FREE, BASIC, PREMIUM, ENTERPRISE
    }

    public enum Status {
        ACTIVE, INACTIVE, SUSPENDED, DELETED
    }

    public enum PaymentMethod {
        MEDX, MPESA, EMOLA
    }

    // ================== Nested Objects ==================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Security {
        private String passwordHash = "";             // bcrypt hash
        private boolean twoFactorEnabled = false;    // 2FA desativado por padrão
        private String twoFactorSecret = "";         // chave TOTP
        private String twoFactorTempCode = "";       // código temporário
        private Instant twoFactorExpiry = null;      // expiração
        private List<String> lastLoginIps = new ArrayList<>();
        private Instant lastLoginAt = null;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubscriptionPlan {
        private PlanType type = PlanType.FREE;       // plano inicial FREE
        private Instant startDate = Instant.now();
        private Instant expiryDate = Instant.now().plusSeconds(30L*24*3600); // 30 dias por padrão
        private boolean autoRenew = false;
        private PaymentMethod paymentMethod = null;           // método de pagamento vazio
        private int maxDevices = 1;                  // limite inicial de 1 dispositivo
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Preferences {
        private String language = "pt";             // português padrão
        private String theme = "light";             // tema claro padrão
        private boolean emailNotifications = true;
        private boolean smsNotifications = false;
        private boolean pushNotifications = true;
        private String currency = "MZN";            // moeda padrão
    }
}
