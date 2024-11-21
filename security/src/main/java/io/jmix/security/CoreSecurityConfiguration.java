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

package io.jmix.security;

import io.jmix.security.authentication.CurrentAuthentication;
import io.jmix.security.check.PostAuthenticationChecks;
import io.jmix.security.check.PreAuthenticationChecks;
import io.jmix.security.logging.LogMdcFilter;
import io.jmix.security.rememberme.JmixRememberMeServices;
import io.jmix.security.rememberme.RememberMeProperties;
import io.jmix.security.session.SessionProperties;
import io.jmix.security.user.UserRepository;
import io.jmix.security.util.JmixHttpSecurityUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@RequiredArgsConstructor
@AutoConfiguration
public class CoreSecurityConfiguration {
    public static final String SECURITY_CONFIGURER_QUALIFIER = "standard-security";
    private final SessionProperties sessionProperties;
    private final RememberMeProperties rememberMeProperties;
    private final UserRepository userRepository;

    @Bean
    public RememberMeServices rememberMeServices(PersistentTokenRepository rememberMeTokenRepository) {
        JmixRememberMeServices rememberMeServices =
                new JmixRememberMeServices(rememberMeProperties.getKey(), userRepository, rememberMeTokenRepository);
        rememberMeServices.setTokenValiditySeconds(rememberMeProperties.getTokenValiditySeconds());
        rememberMeServices.setParameter(rememberMeProperties.getParameter());
        return rememberMeServices;
    }

    @Primary
    @Bean
    public SessionAuthenticationStrategy sessionControlAuthenticationStrategy() {
        RegisterSessionAuthenticationStrategy registerSessionAuthenticationStrategy
                = new RegisterSessionAuthenticationStrategy(sessionRegistry());
        ConcurrentSessionControlAuthenticationStrategy concurrentSessionControlStrategy
                = new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry());
        concurrentSessionControlStrategy.setMaximumSessions(sessionProperties.getMaximumSessionsPerUser());

        List<SessionAuthenticationStrategy> strategies = new LinkedList<>();
        strategies.add(registerSessionAuthenticationStrategy);
        strategies.add(concurrentSessionControlStrategy);

        return new CompositeSessionAuthenticationStrategy(strategies);
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public PreAuthenticationChecks preAuthenticationChecks() {
        return new PreAuthenticationChecks();
    }

    @Bean
    public PostAuthenticationChecks postAuthenticationChecks() {
        return new PostAuthenticationChecks();
    }

    @Bean
    public DefaultAuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher publisher) {
        return new DefaultAuthenticationEventPublisher(publisher);
    }

    @Bean("sec_StandardSecurityFilterChain")
    @Order(JmixSecurityFilterOrder.STANDARD_SECURITY)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JmixHttpSecurityUtils.configureAnonymous(http);
        JmixHttpSecurityUtils.configureSessionManagement(http);
        JmixHttpSecurityUtils.configureFrameOptions(http);
        JmixHttpSecurityUtils.configureRememberMe(http);
        JmixHttpSecurityUtils.configurePermitAll(http);

        http.formLogin(Customizer.withDefaults());

        JmixHttpSecurityUtils.applySecurityConfigurersWithQualifier(http, SECURITY_CONFIGURER_QUALIFIER);
        return http.build();
    }

    @Bean
    @Order(JmixSecurityFilterOrder.MDC)
    public FilterRegistrationBean<LogMdcFilter> logMdcFilterFilterRegistrationBean(CurrentAuthentication currentAuthentication) {
        LogMdcFilter logMdcFilter = new LogMdcFilter(currentAuthentication);
        FilterRegistrationBean<LogMdcFilter> filterRegistration = new FilterRegistrationBean<>(logMdcFilter);
        filterRegistration.setUrlPatterns(Set.of("/*"));
        return filterRegistration;
    }

}