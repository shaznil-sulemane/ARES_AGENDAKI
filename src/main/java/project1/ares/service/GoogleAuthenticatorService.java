package project1.ares.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthenticatorService {

    private final GoogleAuthenticator gAuth;

    public GoogleAuthenticatorService() {
        this.gAuth = new GoogleAuthenticator();
    }

    /**
     * Gera um secret único para o usuário (guardar no Mongo)
     */
    public String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /**
     * Gera a URL para configurar o app Google Authenticator
     */
    public String generateQRUrl(String username, String secret) {
        return String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s",
                username,
                secret,
                "KrypthonApp" // Nome da app/empresa
        );
    }

    /**
     * Verifica se o código TOTP é válido
     */
    public boolean authorize(String secret, String code) {
        try {
            int verificationCode = Integer.parseInt(code);
            return gAuth.authorize(secret, verificationCode);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

