/*
 * Copyright 2019 Haulmont.
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

package io.jmix.security.authentication.impl;

import com.google.common.base.Strings;
import io.jmix.core.JmixOrder;
import io.jmix.security.authentication.SystemAuthenticatorSupport;
import io.jmix.security.authentication.SystemAuthenticator;
import io.jmix.security.authentication.token.SystemAuthenticationToken;
import io.jmix.security.util.SecurityContextHelper;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class SystemAuthenticatorImpl extends SystemAuthenticatorSupport implements SystemAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(SystemAuthenticatorImpl.class);

    protected final AuthenticationManager authenticationManager;

    @EventListener
    @Order(JmixOrder.HIGHEST_PRECEDENCE + 5)
    protected void beginServerSessionOnStartup(ContextRefreshedEvent event) {
        if (authenticationManager!=null) {
            begin();
        }
    }

    @EventListener
    @Order(JmixOrder.LOWEST_PRECEDENCE - 5)
    protected void endServerSessionOnStartup(ContextRefreshedEvent event) {
        if (authenticationManager!=null) {
            end();
        }
    }

    @Override
    public Authentication begin(@Nullable String login) {
        if (authenticationManager==null) {
            throw new IllegalStateException("AuthenticationManager is not defined");
        }

        pushAuthentication(SecurityContextHelper.getAuthentication());
        try {
            Authentication authentication;

            if (!Strings.isNullOrEmpty(login)) {
                log.info("Authenticating as {}", login);
                Authentication authToken = new SystemAuthenticationToken(login);
                authentication = authenticationManager.authenticate(authToken);
            } else {
                log.info("Authenticating as system");
                Authentication authToken = new SystemAuthenticationToken(null);
                authentication = authenticationManager.authenticate(authToken);
            }

            SecurityContextHelper.setAuthentication(authentication);

            return authentication;
        } catch (AuthenticationException e) {
            pollAuthentication();
            throw e;
        }
    }

    @Override
    public Authentication begin() {
        return begin(null);
    }

    @Override
    public void end() {
        log.trace("Set previous Authentication");
        Authentication previous = pollAuthentication();
        SecurityContextHelper.setAuthentication(previous);
    }

    @Override
    public <T> T withUser(@Nullable String login, Callable<T> operation) {
        begin(login);
        try {
            return operation.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            end();
        }
    }

    @Override
    public void runWithUser(@Nullable String login, Runnable operation) {
        begin(login);
        try {
            operation.run();
        } finally {
            end();
        }
    }

    @Override
    public <T> T withSystem(Callable<T> operation) {
        begin();
        try {
            return operation.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            end();
        }
    }

    @Override
    public void runWithSystem(Runnable operation) {
        begin();
        try {
            operation.run();
        } finally {
            end();
        }
    }
}