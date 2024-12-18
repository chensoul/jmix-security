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
import io.jmix.security.user.UserRepository;
import io.jmix.security.user.substitution.UserSubstitution;
import io.jmix.security.user.substitution.UserSubstitutionManager;
import io.jmix.security.user.substitution.UserSubstitutionProvider;
import io.jmix.security.user.substitution.event.UserSubstitutedEvent;
import io.jmix.security.util.SecurityContextHelper;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;

@RequiredArgsConstructor
public class DefaultUserSubstitutionManager implements UserSubstitutionManager {
    @Nullable
    protected final UserRepository userRepository;
    @Nullable
    protected final AuthenticationManager authenticationManager;
    protected final CurrentAuthentication currentAuthentication;
    protected final ApplicationEventPublisher eventPublisher;
    protected final Collection<UserSubstitutionProvider> userSubstitutionProviders;


    @Override
    public List<UserDetails> getCurrentSubstitutedUsers() {
        return getSubstitutedUsers(currentAuthentication.getUser().getUsername(), new Date());
    }

    @Override
    public List<UserDetails> getSubstitutedUsers(String username, Date date) {
        if (userRepository==null) {
            throw new IllegalStateException("UserRepository is not defined");
        }
        return getUserSubstitutions(username, date).stream()
                .map(userSubstitution -> userRepository.loadUserByUsername(userSubstitution.getSubstitutedUsername()))
                .collect(Collectors.toList());
    }

    protected Collection<UserSubstitution> getUserSubstitutions(String username, Date date) {
        return userSubstitutionProviders.stream()
                .flatMap(provider -> provider.getUserSubstitutions(username, date).stream())
                .collect(Collectors.toList());
    }

    /**
     * Check {@link UserSubstitution} collection and performs user substitution
     *
     * @throws IllegalArgumentException if current user isn't allowed to substitute user with specified name
     */
    public void substituteUser(String substitutedUserName) {
        if (!canSubstitute(currentAuthentication.getUser().getUsername(), substitutedUserName)) {
            throw new IllegalArgumentException(
                    String.format("User '%s' cannot substitute '%s'",
                            currentAuthentication.getUser().getUsername(),
                            substitutedUserName));
        }

        if (authenticationManager==null) {
            throw new IllegalStateException("AuthenticationManager is not defined");
        }

        SubstitutedUserAuthenticationToken authentication = (SubstitutedUserAuthenticationToken) authenticationManager.authenticate(
                new SubstitutedUserAuthenticationToken(currentAuthentication.getAuthentication(), substitutedUserName));

        SecurityContextHelper.setAuthentication(authentication);
        eventPublisher.publishEvent(new UserSubstitutedEvent((UserDetails) authentication.getPrincipal(), (UserDetails) authentication.getSubstitutedPrincipal()));
    }

    protected boolean canSubstitute(String userName, String substitutedUserName) {
        return userName.equals(substitutedUserName)
                || getUserSubstitutions(userName, new Date()).stream()
                .anyMatch(userSubstitution -> userSubstitution.getSubstitutedUsername().equals(substitutedUserName));
    }
}
