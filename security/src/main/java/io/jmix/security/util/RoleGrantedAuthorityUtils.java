/*
 * Copyright 2022 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.security.util;

import io.jmix.security.SecurityProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Utility class for working with Jmix-related {@link GrantedAuthority}. Use this class when you need to
 * programmatically create an instance of the granted authority for resource or row-level role.
 */
@Component("sec_RoleGrantedAuthorityUtils")
public class RoleGrantedAuthorityUtils {

    private GrantedAuthorityDefaults grantedAuthorityDefaults;
    private final SecurityProperties securityProperties;
    private String defaultRolePrefix = "ROLE_";

    public RoleGrantedAuthorityUtils(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Autowired(required = false)
    public void setGrantedAuthorityDefaults(GrantedAuthorityDefaults grantedAuthorityDefaults) {
        this.grantedAuthorityDefaults = grantedAuthorityDefaults;
    }

    @PostConstruct
    public void init() {
        if (grantedAuthorityDefaults != null) {
            defaultRolePrefix = grantedAuthorityDefaults.getRolePrefix();
        }
    }

    public GrantedAuthority createRowLevelRoleGrantedAuthority(String rowLevelRoleCode) {
        return new SimpleGrantedAuthority(getDefaultRowLevelRolePrefix() + rowLevelRoleCode);
    }

    /**
     * Returns the role prefix for the resource role. It is taken from the {@link GrantedAuthorityDefaults} if the bean
     * of this type is defined. Otherwise, the default ROLE_ value is returned.
     */
    public String getDefaultRolePrefix() {
        return defaultRolePrefix;
    }

    /**
     * Returns the role prefix for the row-level role (ROW_LEVEL_ROLE_ by default)
     */
    public String getDefaultRowLevelRolePrefix() {
        return securityProperties.getDefaultRowLevelRolePrefix();
    }
}
