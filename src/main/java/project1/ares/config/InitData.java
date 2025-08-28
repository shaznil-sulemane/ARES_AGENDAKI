package project1.ares.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import project1.ares.model.User;
import project1.ares.repository.UserRepository;
import reactor.core.publisher.Mono;

@Configuration
public class InitData {

    private static final Logger log = LoggerFactory.getLogger(InitData.class);

    @Bean
    public ApplicationRunner init(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            User user = new User();
            user.setUsername("admin");
            user.setId("0000");
            user.setPhoneNumber("258833711282");
            user.setFullName("ARES Group Lda");
            user.setUsername("aresgrouplda");
            user.setRole(User.Role.ADMIN);
            user.setEmail("aresgrouplda@gmail.com");
            user.getSecurity().setPasswordHash(passwordEncoder.encode("ARESGroup@2025"));

            userRepository.findById("0000")
                    .map(user1 -> user1)
                    .switchIfEmpty(Mono.defer(() -> userRepository.save(user)))
                    .subscribe(user1 -> {}, throwable -> {});

        };
    }
}
