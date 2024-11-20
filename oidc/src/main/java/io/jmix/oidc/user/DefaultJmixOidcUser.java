package io.jmix.oidc.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

/**
 * The default implementation of {@link JmixOidcUser} that wraps the {@link OidcUser} provided by the OpenID Connect 1.0
 * Provider and delegates some method invocations to the wrapped {@code OidcUser}.
 */
public class DefaultJmixOidcUser implements JmixOidcUser, HasOidcUserDelegate {

    private OidcUser delegate;

    private Collection<? extends GrantedAuthority> authorities;

    @Override
    public OidcUser getDelegate() {
        return delegate;
    }

    @Override
    public void setDelegate(OidcUser delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getPassword() {
        //todo empty password?
        return "";
    }

    @Override
    public String getUsername() {
        return delegate.getName();
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
        return true;
    }

    @Override
    public Map<String, Object> getClaims() {
        return delegate.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return delegate.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return delegate.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }
}
