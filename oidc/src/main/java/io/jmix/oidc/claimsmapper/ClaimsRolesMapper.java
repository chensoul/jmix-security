package io.jmix.oidc.claimsmapper;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;

/**
 * Mapper of claims received from the OpenID Provider into Jmix resource roles and row-level roles. Some {@link io.jmix.oidc.usermapper.OidcUserMapper}
 * implementations delegate roles mapping to the instance of this interface. If you want to replace standard claims mapper
 * with your own one, then register a spring bean implementing the current interface in your application.
 *
 * @see io.jmix.oidc.usermapper.OidcUserMapper
 */
public interface ClaimsRolesMapper {

    Collection<? extends GrantedAuthority> toGrantedAuthorities(Map<String, Object> claims);
}
