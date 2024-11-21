/*
 * Copyright 2024 Haulmont.
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

import io.jmix.security.session.SessionProperties;
import io.jmix.security.user.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

/**
 * Utility class for Jmix-specific configuration of {@link HttpSecurity}.
 */
public class JmixHttpSecurityUtils {

    /**
     * Configures frame options to use the same origin.
     */
    public static void configureFrameOptions(HttpSecurity http) throws Exception {
        http.headers(headers ->
                headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
    }

    /**
     * Configures anonymous access to the application for the given {@link HttpSecurity} instance. Anonymous user is
     * taken from the {@link UserRepository#getAnonymousUser()}.
     */
    public static void configureAnonymous(HttpSecurity http) throws Exception {
        ApplicationContext applicationContext = http.getSharedObject(ApplicationContext.class);
        UserRepository userRepository = applicationContext.getBean(UserRepository.class);

        http.anonymous(anonymousConfigurer -> {
            anonymousConfigurer.key("anonymous-key").principal(userRepository.getAnonymousUser());
            Collection<? extends GrantedAuthority> anonymousAuthorities = userRepository.getAnonymousUser().getAuthorities();
            if (!anonymousAuthorities.isEmpty()) {
                anonymousConfigurer.authorities(new ArrayList<>(userRepository.getAnonymousUser().getAuthorities()));
            }
        });
    }

    /**
     * Configures session management using settings defined in the {@link SessionProperties} for the given
     * {@link HttpSecurity} instance.
     */
    public static void configureSessionManagement(HttpSecurity http) {
        try {
            ApplicationContext applicationContext = http.getSharedObject(ApplicationContext.class);

            SessionAuthenticationStrategy sessionAuthenticationStrategy = applicationContext.getBean(SessionAuthenticationStrategy.class);
            SessionRegistry sessionRegistry = applicationContext.getBean(SessionRegistry.class);
            SessionProperties sessionProperties = applicationContext.getBean(SessionProperties.class);

            http.sessionManagement(sessionManagement -> {
                sessionManagement.sessionAuthenticationStrategy(sessionAuthenticationStrategy)
                        .maximumSessions(sessionProperties.getMaximumSessionsPerUser())
                        .sessionRegistry(sessionRegistry);
            });
        } catch (Exception e) {
            throw new RuntimeException("Error while init security", e);
        }
    }

    /**
     * Configures default remember me functionality.
     */
    public static void configureRememberMe(HttpSecurity http) {
        try {
            ApplicationContext applicationContext = http.getSharedObject(ApplicationContext.class);
            RememberMeServices rememberMeServices = applicationContext.getBean(RememberMeServices.class);

            http.rememberMe(rememberMe -> rememberMe.rememberMeServices(rememberMeServices));
        } catch (Exception e) {
            throw new RuntimeException("Error while init security", e);
        }
    }

    public static void configurePermitAll(HttpSecurity http, String... patterns) {
        try {
            http.authorizeHttpRequests(authorize -> {
                authorize.requestMatchers(patterns).permitAll()
                        .anyRequest().authenticated();
            });
        } catch (Exception e) {
            throw new RuntimeException("Error while init security", e);
        }
    }

    public static void applySecurityConfigurersWithQualifier(HttpSecurity http, String qualifier) throws Exception {
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) http.getSharedObject(ApplicationContext.class);
        Map<String, SecurityConfigurer> beans = BeanFactoryAnnotationUtils.qualifiedBeansOfType(applicationContext.getBeanFactory(), SecurityConfigurer.class, qualifier);
        for (SecurityConfigurer configurer : beans.values()) {
            http.apply(configurer);
        }
    }
}
