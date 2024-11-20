package io.jmix.oidc.claimsmapper;

import io.jmix.security.util.RoleGrantedAuthorityUtils;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;

public class BaseClaimsRolesMapper implements ClaimsRolesMapper {

    private static final Logger log = LoggerFactory.getLogger(BaseClaimsRolesMapper.class);

    protected RoleGrantedAuthorityUtils roleGrantedAuthorityUtils;

    //todo setter injection?
    public BaseClaimsRolesMapper(
            RoleGrantedAuthorityUtils roleGrantedAuthorityUtils) {
        this.roleGrantedAuthorityUtils = roleGrantedAuthorityUtils;
    }

    @Override
    public Collection<? extends GrantedAuthority> toGrantedAuthorities(Map<String, Object> claims) {

        return null;
    }
}
