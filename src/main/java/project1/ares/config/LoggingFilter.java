package project1.ares.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import project1.ares.model.Log;
import project1.ares.repository.LogRepository;
import project1.ares.security.JwtUtil;
import project1.ares.service.CustomUserDetailsService;
import reactor.core.publisher.Mono;
import java.time.Instant;

@Component
public class LoggingFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final LogRepository logRepository;

    public LoggingFilter(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService, LogRepository logRepository) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = customUserDetailsService;
        this.logRepository = logRepository;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String ip = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
        String path = request.getURI().toString();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        Instant start = Instant.now();

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUserId(token);

                log.info("Username: " + username);
                return userDetailsService.findByUsername(username)
                        .flatMap(userDetails -> {
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                            return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(new SecurityContextImpl(auth))));
                        })
                        .onErrorResume(e -> {
                            log.warn("Falha ao autenticar '{}': {}", username, e.getMessage());
                            return chain.filter(exchange); // segue sem autenticação
                        })
                        .then(Mono.empty());
            }
        }
        return chain.filter(exchange);
        // Salvando log sempre
        //return processing.doFinally(sig -> saveLog(exchange, method, path, ip, start));
    }
    private void saveLog(ServerWebExchange exchange, String method, String path, String ip, Instant start) {
        ServerHttpResponse response = exchange.getResponse();
        response.getStatusCode();
        int statusCode = response.getStatusCode().value();
        long duration = Instant.now().toEpochMilli() - start.toEpochMilli();

        Log logEntry = new Log();
        logEntry.setUser(ip);
        logEntry.setMessage(String.format("[%s] %s %s | IP: %s | Status: %d | Tempo: %dms",
                Instant.now(), method, path, ip, statusCode, duration));

        logRepository.save(logEntry).subscribe(
                ok -> {},
                err -> log.error("Erro ao salvar log: {}", err.getMessage())
        );
    }

}
