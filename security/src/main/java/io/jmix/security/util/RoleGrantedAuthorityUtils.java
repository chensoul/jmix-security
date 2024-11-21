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

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Utility class for working with Jmix-related {@link GrantedAuthority}. Use this class when you need to
 * programmatically create an instance of the granted authority for resource or row-level role.
 */
@RequiredArgsConstructor
@Component("sec_RoleGrantedAuthorityUtils")
public class RoleGrantedAuthorityUtils {
    private String defaultRolePrefix = "ROLE_";

    public GrantedAuthority createRowGrantedAuthority(String rowLevelRoleCode) {
        return new SimpleGrantedAuthority(defaultRolePrefix + rowLevelRoleCode);
    }
}
