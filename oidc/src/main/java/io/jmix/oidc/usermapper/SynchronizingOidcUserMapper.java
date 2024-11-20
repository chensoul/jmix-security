package io.jmix.oidc.usermapper;

import io.jmix.security.user.UserRepository;
import io.jmix.security.util.RoleGrantedAuthorityUtils;
import io.jmix.oidc.claimsmapper.ClaimsRolesMapper;
import io.jmix.oidc.user.JmixOidcUser;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Implementation of the {@link OidcUserMapper} that not only maps the external user object to the persistent user
 * entity, but also stores the user and optionally their role assignment to the database.
 *
 * @param <T>
 */
//todo make the class generic and move it to jmix-security in order to share it with jmix-ldap and jmix-oidc
public abstract class SynchronizingOidcUserMapper<T extends JmixOidcUser> extends BaseOidcUserMapper<T> {

    private static final Logger log = LoggerFactory.getLogger(SynchronizingOidcUserMapper.class);


    protected UserRepository userRepository;

    protected ClaimsRolesMapper claimsRolesMapper;

    protected boolean synchronizeRoleAssignments;

    protected RoleGrantedAuthorityUtils roleGrantedAuthorityUtils;

    public SynchronizingOidcUserMapper(
            UserRepository userRepository,
            ClaimsRolesMapper claimsRolesMapper,
            RoleGrantedAuthorityUtils roleGrantedAuthorityUtils) {
        this.userRepository = userRepository;
        this.claimsRolesMapper = claimsRolesMapper;
        this.roleGrantedAuthorityUtils = roleGrantedAuthorityUtils;
    }

    /**
     * Returns a class of the user used by the application. This user is set to the security context.
     */
    protected abstract Class<T> getApplicationUserClass();

    /**
     * Extracts username from the {@code oidcUser}
     */
    @Override
    protected String getOidcUserUsername(OidcUser oidcUser) {
        return oidcUser.getName();
    }

    @Override
    protected T initJmixUser(OidcUser oidcUser) {
        String username = getOidcUserUsername(oidcUser);
        T jmixUserDetails = (T) userRepository.loadUserByUsername(username);
        return jmixUserDetails;
    }

    @Override
    protected void populateUserAuthorities(OidcUser oidcUser, T jmixUser) {
        Collection<? extends GrantedAuthority> grantedAuthorities = claimsRolesMapper.toGrantedAuthorities(oidcUser.getClaims());
        jmixUser.setAuthorities(grantedAuthorities);
    }

    @Override
    protected void performAdditionalModifications(OidcUser oidcUser, T jmixUser) {
        super.performAdditionalModifications(oidcUser, jmixUser);
        saveJmixUserAndRoleAssignments(oidcUser, jmixUser);
    }

    protected void saveJmixUserAndRoleAssignments(OidcUser oidcUser, T jmixUser) {
        //TODO
    }


    /**
     * Enables role assignment entities synchronization. If true then role assignment entities will be stored to the
     * database.
     */
    public void setSynchronizeRoleAssignments(boolean synchronizeRoleAssignments) {
        this.synchronizeRoleAssignments = synchronizeRoleAssignments;
    }

    public boolean isSynchronizeRoleAssignments() {
        return synchronizeRoleAssignments;
    }
}
