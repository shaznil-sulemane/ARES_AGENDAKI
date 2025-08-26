package project1.ares.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import reactor.core.publisher.Mono;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Trata exceções de validação (ex: usuário já existe, email duplicado, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleIllegalArgument(IllegalArgumentException ex) {
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(false, ex.getMessage(), null))
        );
    }

    // Trata exceções de autorização (ex: usuário já existe, email duplicado, etc.)
    @ExceptionHandler(AuthorizationDeniedException.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleIllegalArgument(AuthorizationDeniedException ex) {
        return Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse<>(false, ex.getMessage(), null))
        );
    }

    // Trata erros inesperados do servidor
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Object>>> handleGeneral(Exception ex) {
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponse<>(false, "Ocorreu um erro inesperado. Tente novamente mais tarde." + ex.getMessage(), null))
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Mono<ResponseEntity<ApiResponse<?>>> handleUnauthorized(UnauthorizedException ex) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>(false, ex.getMessage(), null)));
    }


    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

}
