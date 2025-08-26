package project1.ares.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import project1.ares.model.User;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@AllArgsConstructor
@Builder
@Getter
public class CustomUserDetails implements UserDetails {

    private final String id;             // ID do usuário
    private final String username;
    private final String password;
    private final User.Role role; // Roles do usuário
    private final boolean active;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (role == null) return Collections.emptySet();
        return Set.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return active; }
    @Override public boolean isAccountNonLocked() { return active; }
    @Override public boolean isCredentialsNonExpired() { return active; }
    @Override public boolean isEnabled() { return active; }
}