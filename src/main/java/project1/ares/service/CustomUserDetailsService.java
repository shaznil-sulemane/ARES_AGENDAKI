package project1.ares.service;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import project1.ares.config.CustomUserDetails;
import project1.ares.model.User;
import project1.ares.repository.UserRepository;
import reactor.core.publisher.Mono;

@Service
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByIdentifier(username)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException("Usuário não encontrado")))
                .map(user -> CustomUserDetails.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .active(user.getStatus().equals(User.Status.ACTIVE))
                        .password(user.getSecurity().getPasswordHash())
                        .build());
    }
}
