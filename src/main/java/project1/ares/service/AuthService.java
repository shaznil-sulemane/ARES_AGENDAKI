package project1.ares.service;

import com.mongodb.DuplicateKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import project1.ares.model.User;
import project1.ares.repository.UserRepository;
import project1.ares.security.JwtUtil;
import project1.ares.util.Regex;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleAuthenticatorService googleAuthenticatorService;
    private final SequenceGeneratorService sequenceGeneratorService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, GoogleAuthenticatorService googleAuthenticatorService, SequenceGeneratorService sequenceGeneratorService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.googleAuthenticatorService = googleAuthenticatorService;
        this.sequenceGeneratorService = sequenceGeneratorService;
    }



    public Mono<JwtUtil.Token> signup(Map<String, String> body) {
        String username = body.get("username").toLowerCase().trim();
        String fullname = body.get("fullname").toLowerCase().trim();
        String email = body.get("email").toLowerCase().trim();
        String phone = body.get("phone").toLowerCase().trim();
        String rawPassword = body.get("password");
        String role = body.get("role").toUpperCase().trim();

        try {
            if (User.Role.valueOf(role) == User.Role.ADMIN) {
                return Mono.error(new IllegalArgumentException("Papel invalido."));
            }
        } catch (Exception e) {
            return Mono.error(new IllegalArgumentException("Papel invalido."));
        }

        if(
                !Regex.isValidUsername(username.toLowerCase().trim()) ||
                !Regex.isValidEmail(email.toLowerCase().trim()) ||
                !Regex.isValidPhone(phone.toLowerCase().trim())
        ) {
            return Mono.error(new IllegalArgumentException("Dados inválidos."));
        }

        return userRepository.existsByUsername(username)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Usuário já existe"));
                    }
                    return userRepository.existsByEmail(email);
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Email já existe"));
                    }
                    return userRepository.existsByPhoneNumber(phone);
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Telefone já existe"));
                    }

                    User user = new User();
                    user.setUsername(username);
                    user.setEmail(email);
                    user.setPhoneNumber(phone);
                    user.getSecurity().setPasswordHash(passwordEncoder.encode(rawPassword));
                    user.setRole(User.Role.valueOf(role));
                    user.setFullName(fullname);

                    return sequenceGeneratorService
                            .generateUserId()
                            .flatMap(usId -> {
                                user.setId(usId);
                                return userRepository
                                        .save(user)
                                        .map(savedUser -> jwtUtil.generateToken(savedUser.getId(), savedUser.getRole().name()));
                            });
                });
    }

    public Mono<JwtUtil.Token> login(Map<String, String> body) {

        String identifier = body.get("identifier");
        String password = body.get("password");

        Mono<Boolean> isAuthenticated = authenticate(identifier, password);
        Mono<Boolean> isTwoFactorEnabled = isTwoFactorEnabled(identifier);
        Mono<Boolean> canLogin = canLogin(identifier);
        Mono<Boolean> isDeleted = isDeleted(identifier);
        Mono<User.Role> userRole = getRole(identifier);

        return Mono.zip(isAuthenticated, isTwoFactorEnabled, canLogin, isDeleted, userRole)
                .flatMap(results -> {
                    boolean authenticated = results.getT1();
                    boolean twoFactorEnabled = results.getT2();
                    boolean login = results.getT3();
                    boolean deleted = results.getT4();
                    User.Role role = results.getT5();

                    if(deleted) {
                        return Mono.error(new IllegalArgumentException("Usuário deletado."));
                    }

                    if(!login) {
                        return Mono.error(new AuthorizationDeniedException("Usuário suspenso."));
                    }

                    if (!authenticated) {
                        return Mono.error(new IllegalArgumentException("Usuário ou senha invalido."));
                    }

                    if (twoFactorEnabled) {
                        log.info("Usuário possui dois factores.");
                        String key = googleAuthenticatorService.generateSecretKey();
                        log.info(googleAuthenticatorService.generateQRUrl(identifier, key));
                        return Mono.just(jwtUtil.generateToken(identifier, role.name()));
                    }

                    return Mono.just(jwtUtil.generateToken(identifier, role.name()));
                })
                .onErrorResume(ex -> Mono.error(new IllegalArgumentException("Usuário ou senha inválidos.")));
    }

    public Mono<JwtUtil.Token> verify2FA(String username, String code) {
        return userRepository.findByUsername(username)
                .filter(user -> googleAuthenticatorService.authorize(user.getSecurity().getTwoFactorTempCode(), code))
                .map(user -> jwtUtil.generateToken(user.getUsername(), user.getRole().name()));
    }

    public Mono<Boolean> checkAvailability(String field, String value) {
        switch (field.toLowerCase()) {
            case "username":
                return userRepository.existsByUsername(value.toLowerCase().trim())
                        .map(exists -> !exists);
            case "email":
                return userRepository.existsByEmail(value.toLowerCase().trim())
                        .map(exists -> !exists);
            case "phone":
            case "phonenumber":
                return userRepository.existsByPhoneNumber(value.toLowerCase().trim())
                        .map(exists -> !exists);
            default:
                return Mono.error(new IllegalArgumentException("Campo inválido. Use: username, email ou phone."));
        }
    }


    private Mono<Boolean> authenticate(String identifier, String password) {
        return userRepository.findByIdentifier(identifier)
                .map(user -> passwordEncoder.matches(password, user.getSecurity().getPasswordHash()))
                .switchIfEmpty(Mono.empty());
    }

    private Mono<Boolean> isTwoFactorEnabled(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .map(user -> user.getSecurity().isTwoFactorEnabled())
                .switchIfEmpty(Mono.empty());
    }

    private Mono<Boolean> isDeleted(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .map(user -> (user.getStatus().equals(User.Status.DELETED)))
                .switchIfEmpty(Mono.empty());
    }

    private Mono<Boolean> canLogin(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .map(user -> !(user.getStatus().equals(User.Status.SUSPENDED)))
                .switchIfEmpty(Mono.empty());
    }

    private Mono<User.Role> getRole(String identifier) {
        return userRepository.findByIdentifier(identifier)
                .map(User::getRole)
                .switchIfEmpty(Mono.empty());
    }
}
