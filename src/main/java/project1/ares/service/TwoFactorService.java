package project1.ares.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import org.springframework.stereotype.Service;

@Service
public class TwoFactorService {

    private GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // Gera segredo para TOTP
    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    // Valida código do usuário
    public boolean validateCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }
}
