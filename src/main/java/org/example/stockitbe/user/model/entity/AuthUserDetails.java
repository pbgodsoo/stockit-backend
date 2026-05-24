package org.example.stockitbe.user.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class AuthUserDetails implements UserDetails {
    private String employeeCode;
    private String password;
    private String name;
    private UserStatus status;
    private UserRole role;
    private String locationCode;
    private String locationName;

    public static AuthUserDetails from(User entity) {
        return AuthUserDetails.builder()
                .employeeCode(entity.getEmployeeCode())
                .password(entity.getPassword())
                .name(entity.getName())
                .role(entity.getRole())
                .status(entity.getStatus())
                .locationCode(entity.getLocationCode())
                .locationName(entity.getLocationName())
                .build();
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override                                       // 대기, 승인, 거절
    public boolean isEnabled() {
        return status == UserStatus.APPROVED;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return employeeCode;
    }
}
