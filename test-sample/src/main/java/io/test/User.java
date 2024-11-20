package io.test;

import io.jmix.core.HasTimeZone;
import io.jmix.security.user.JmixUserDetails;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

@Entity
@Table(name = "USER_", indexes = {
        @Index(name = "IDX_USER__ON_USERNAME", columnList = "USERNAME", unique = true)
})
@Data
public class User implements JmixUserDetails, HasTimeZone {
    @Id
    @Column(name = "ID")
    private UUID id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @Column(name = "USERNAME", nullable = false)
    protected String username;

    @Column(name = "PASSWORD")
    protected String password;

    @Column(name = "FIRST_NAME")
    protected String firstName;

    @Column(name = "LAST_NAME")
    protected String lastName;

    @Email
    @Column(name = "EMAIL")
    protected String email;

    @Column(name = "ACTIVE")
    protected Boolean active = true;

    @Column(name = "TIME_ZONE_ID")
    protected String timeZoneId;

    @Transient
    protected Collection<? extends GrantedAuthority> authorities;

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities!=null ? authorities:Collections.emptyList();
    }

    @Override
    public void setAuthorities(final Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
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

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(active);
    }

    public String getDisplayName() {
        return String.format("%s %s [%s]", (firstName!=null ? firstName:""),
                (lastName!=null ? lastName:""), username).trim();
    }

}