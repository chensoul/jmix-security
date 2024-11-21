/*
 * Copyright 2020 Haulmont.
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

package io.jmix.security.user;

import com.google.common.base.Strings;
import io.jmix.core.util.Preconditions;
import io.jmix.security.user.event.SingleUserPasswordChangeEvent;
import io.jmix.security.user.event.UserPasswordResetEvent;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

public abstract class AbstractDatabaseUserRepository<T extends UserDetails> implements UserRepository, UserManager {
    private T systemUser;
    private T anonymousUser;

    @Autowired
    protected PasswordEncoder passwordEncoder;
    @Autowired
    protected PersistentTokenRepository tokenRepository;
    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @PostConstruct
    private void init() {
        systemUser = createSystemUser();
        anonymousUser = createAnonymousUser();
    }

    /**
     * Creates the built-in 'system' user.
     */
    protected abstract T createSystemUser();

    /**
     * Creates the built-in 'anonymous' user.
     */
    protected abstract T createAnonymousUser();


    @Override
    public T getSystemUser() {
        return systemUser;
    }

    @Override
    public T getAnonymousUser() {
        return anonymousUser;
    }

    @Override
    public T loadUserByUsername(String username) throws UsernameNotFoundException {
        List<T> users = loadUsersByUsernameFromDatabase(username);
        if (!users.isEmpty()) {
            T user = users.get(0);
            return user;
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    protected abstract List<T> loadUsersByUsernameFromDatabase(String username);

    @Override
    public T changePassword(String userName, @Nullable String oldPassword, @Nullable String newPassword,
                            boolean saveChanges) throws PasswordNotMatchException {
        Preconditions.checkNotNullArgument(userName, "Null userName");
        Preconditions.checkNotNullArgument(newPassword, "Null new password");
        T userDetails = loadUserByUsername(userName);
        changePassword(userDetails, oldPassword, newPassword);

        if (saveChanges) {
            saveUser(userDetails);
            eventPublisher.publishEvent(new SingleUserPasswordChangeEvent(userName, newPassword));
        }

        return userDetails;
    }

    protected abstract void saveUser(T userDetails);

    protected abstract void saveUser(T userDetails, String password);

    private void changePassword(T userDetails, @Nullable String oldPassword, @Nullable String newPassword)
            throws PasswordNotMatchException {
        if (!Strings.isNullOrEmpty(userDetails.getPassword())
                && passwordEncoder.matches(newPassword, userDetails.getPassword())
                || oldPassword!=null && !passwordEncoder.matches(oldPassword, userDetails.getPassword())) {
            throw new PasswordNotMatchException();
        }

        saveUser(userDetails, passwordEncoder.encode(newPassword));
    }

    @Override
    public Map<UserDetails, String> resetPasswords(Set<UserDetails> users, boolean saveChanges) {
        Map<UserDetails, String> usernamePasswordMap = new LinkedHashMap<>();

        for (UserDetails user : users) {
            String newPassword;
            boolean success = false;
            T userDetails = null;
            do {
                newPassword = generateRandomPassword();
                try {
                    userDetails = loadUserByUsername(user.getUsername());
                    changePassword(userDetails, null, newPassword);
                } catch (PasswordNotMatchException e) {
                    continue;
                }
                success = true;
            } while (!success);

            usernamePasswordMap.put(userDetails, newPassword);

            if (saveChanges) {
                saveUser(userDetails);
                resetRememberMe(users);
            }
        }

        if (saveChanges) {
            eventPublisher.publishEvent(new UserPasswordResetEvent(usernamePasswordMap.entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().getUsername(), Map.Entry::getValue))));
        }

        return usernamePasswordMap;
    }

    public void resetRememberMe(Collection<UserDetails> users) {
        for (UserDetails user : users) {
            tokenRepository.removeUserTokens(user.getUsername());
        }
    }

    private String generateRandomPassword() {
        SecureRandom random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to load SHA1PRNG", e);
        }
        byte[] passwordBytes = new byte[6];
        random.nextBytes(passwordBytes);
        return new String(Base64.getEncoder().encode(passwordBytes), StandardCharsets.UTF_8).replace("=", "");
    }
}
