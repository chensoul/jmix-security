/*
 * Copyright 2021 Haulmont.
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

package io.jmix.security.user.substitution.impl;

import io.jmix.security.authentication.CurrentAuthentication;
import io.jmix.security.authentication.token.SubstitutedUserAuthenticationToken;
import io.jmix.security.user.substitution.CurrentUserSubstitution;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
public class DefaultCurrentUserSubstitution implements CurrentUserSubstitution {

    protected final CurrentAuthentication currentAuthentication;


    @Override
    public UserDetails getAuthenticatedUser() {
        return currentAuthentication.getUser();
    }

    @Override
    public UserDetails getSubstitutedUser() {
        if (!currentAuthentication.isSet()) {
            return null;
        }
        Authentication authentication = currentAuthentication.getAuthentication();
        if (SubstitutedUserAuthenticationToken.class.isAssignableFrom(authentication.getClass())) {
            Object substitutedPrincipal = ((SubstitutedUserAuthenticationToken) authentication).getSubstitutedPrincipal();
            if (substitutedPrincipal instanceof UserDetails) {
                return (UserDetails) substitutedPrincipal;
            } else {
                throw new RuntimeException("Substituted principal must be UserDetails");
            }
        }
        return null;
    }

    @Override
    public UserDetails getEffectiveUser() {
        UserDetails substitutedUser = getSubstitutedUser();
        return substitutedUser!=null ? substitutedUser:getAuthenticatedUser();
    }
}