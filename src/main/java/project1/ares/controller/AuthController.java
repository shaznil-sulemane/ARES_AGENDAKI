package project1.ares.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project1.ares.config.ApiResponse;
import project1.ares.config.CustomUserDetails;
import project1.ares.model.User;
import project1.ares.repository.UserRepository;
import project1.ares.security.JwtUtil;
import project1.ares.service.AuthService;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<JwtUtil.Token>>> login(@RequestBody Map<String, String> request) {
        return authService.login(request)
                .map(token -> ResponseEntity.ok(new ApiResponse<>(true, "Login bem-sucedido", token)))
                .switchIfEmpty(
                        Mono.just(ResponseEntity
                                .status(401)
                                .body(new ApiResponse<>(false, "Usuário ou senha inválidos.", null))));
    }

    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponse<JwtUtil.Token>>> signup(@RequestBody Map<String, String> request) {
        return authService.signup(request)
                .map(token -> ResponseEntity.ok(new ApiResponse<>(true, "Cadastro bem-sucedido", token)))
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(401)
                        .body(new ApiResponse<>(false, "Erro ao criar usuário.", null))));
    }

    @PostMapping("/2fa")
    public Mono<ResponseEntity<ApiResponse<JwtUtil.Token>>> verify2FA(@RequestBody Map<String, String> request) {
        return authService.verify2FA(request.get("username"), request.get("code"))
                .map(token -> ResponseEntity.ok(new ApiResponse<>(true, "2FA validado com sucesso", token)))
                .switchIfEmpty(Mono.just(ResponseEntity
                        .status(401)
                        .body(new ApiResponse<>(false, "Código 2FA inválido", null))));
    }

    @GetMapping("/availability")
    public Mono<ResponseEntity<ApiResponse<Boolean>>> checkAvailability(
            @RequestParam String field,
            @RequestParam String value) {

        return authService.checkAvailability(field, value)
                .map(isAvailable -> ResponseEntity.ok(
                        new ApiResponse<>(true, isAvailable ? "Disponível" : "Já existe", isAvailable)
                ))
                .onErrorResume(ex -> Mono.just(ResponseEntity
                        .badRequest()
                        .body(new ApiResponse<>(false, ex.getMessage(), null))));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<User>>> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return userRepository.findByIdentifier(principal.getUsername())
                .map(user -> ResponseEntity.ok(new ApiResponse<>(true, "Usuário encontrado.", user)))
                .switchIfEmpty(
                        Mono.just(ResponseEntity
                                .status(400)
                                .body(new ApiResponse<>(false, "Usuário não encontrado.", null))));
    }

}
