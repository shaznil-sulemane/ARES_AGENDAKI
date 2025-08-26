package project1.ares.config;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import project1.ares.repository.UserRepository;
import reactor.core.publisher.Mono;

@Service
@Primary
public class CustomReactiveUserDetailsService implements ReactiveUserDetailsService {

    private final UserRepository userRepository; // seu repo MongoDB

    public CustomReactiveUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> CustomUserDetails.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .password(user.getSecurity().getPasswordHash())
                        .role(user.getRole())
                        .active(true)
                        .build());
    }
}
