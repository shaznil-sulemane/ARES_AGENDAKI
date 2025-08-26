package project1.ares.util;

public class Regex {

    // ================== Telefone Moçambicano ==================
    // Com ou sem +258, aceita 9 dígitos após o código
    public static final String PHONE_NUMBER = "^(\\+258)?(8[2345679]\\d{7})$";

    // ================== Email ==================
    public static final String EMAIL = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";

    // ================== Senha ==================
    // Mínimo 8 caracteres, pelo menos 1 letra maiúscula, 1 minúscula, 1 número e 1 símbolo
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=[\\]{};':\"\\\\|,.<>/?]).{8,}$";

    // ================== Username ==================
    // Alfanumérico, underscores, 3-20 caracteres
    public static final String USERNAME = "^[A-Za-z0-9_]{3,20}$";

    // ================== Nome completo ==================
    // Letras e espaços, 2-50 caracteres
    public static final String FULL_NAME = "^[A-Za-zÀ-ÿ\\s]{2,50}$";

    // ================== Apelido ==================
    // Letras, números e underscores, 2-15 caracteres
    public static final String NICKNAME = "^[A-Za-z0-9_]{2,15}$";

    // ================== Utilitários ==================
    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches(PHONE_NUMBER);
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches(EMAIL);
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD);
    }

    public static boolean isValidUsername(String username) {
        return username != null && username.matches(USERNAME);
    }

    public static boolean isValidFullName(String fullName) {
        return fullName != null && fullName.matches(FULL_NAME);
    }

    public static boolean isValidNickname(String nickname) {
        return nickname != null && nickname.matches(NICKNAME);
    }
}
