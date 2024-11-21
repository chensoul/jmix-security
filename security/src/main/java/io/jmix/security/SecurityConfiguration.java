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

import io.jmix.core.CoreConfiguration;
import io.jmix.core.annotation.JmixModule;
import io.jmix.security.aspect.AuthenticatedAspect;
import io.jmix.security.authentication.AuthenticationLocaleResolver;
import io.jmix.security.authentication.AuthenticationPrincipalResolver;
import io.jmix.security.authentication.AuthenticationResolver;
import io.jmix.security.authentication.CurrentAuthentication;
import io.jmix.security.authentication.DeviceTimeZoneProvider;
import io.jmix.security.authentication.SystemAuthenticator;
import io.jmix.security.authentication.impl.DefaultAuthenticationManagerSupplierSelector;
import io.jmix.security.authentication.impl.DefaultCurrentAuthentication;
import io.jmix.security.authentication.impl.StandardAuthenticationManagerSupplier;
import io.jmix.security.authentication.impl.SystemAuthenticatorImpl;
import io.jmix.security.authentication.provider.AuthenticationManagerSupplier;
import io.jmix.security.authentication.provider.AuthenticationManagerSupplierSelector;
import io.jmix.security.authentication.provider.StandardAuthenticationProviderProducer;
import io.jmix.security.check.PostAuthenticationChecks;
import io.jmix.security.check.PreAuthenticationChecks;
import io.jmix.security.logging.LogMdcFilter;
import io.jmix.security.user.InMemoryUserRepository;
import io.jmix.security.user.UserRepository;
import io.jmix.security.user.substitution.CurrentUserSubstitution;
import io.jmix.security.user.substitution.UserSubstitutionManager;
import io.jmix.security.user.substitution.UserSubstitutionProvider;
import io.jmix.security.user.substitution.impl.DefaultCurrentUserSubstitution;
import io.jmix.security.user.substitution.impl.DefaultUserSubstitutionManager;
import io.jmix.security.user.substitution.impl.InMemoryUserSubstitutionProvider;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.InMemoryTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@RequiredArgsConstructor
@Configuration
@ComponentScan
@ConfigurationPropertiesScan
@Import(CoreSecurityConfiguration.class)
@JmixModule(dependsOn = CoreConfiguration.class)
@PropertySource(name = "io.jmix.security", value = "classpath:/io/jmix/security/module.properties")
public class SecurityConfiguration {
    /**
     * Global AuthenticationManager
     */
    @Bean("sec_AuthenticationManager")
    public AuthenticationManager authenticationManager(AuthenticationManagerSupplierSelector authenticationManagerSupplierSelector) {
        return authenticationManagerSupplierSelector.getAuthenticationManagerSupplier().getAuthenticationManager();
    }

    @Bean
    @Order(200)
    public AuthenticationManagerSupplier authenticationManagerSupplier(StandardAuthenticationProviderProducer providersProducer,
                                                                       ApplicationEventPublisher publisher) {
        return new StandardAuthenticationManagerSupplier(providersProducer, publisher);
    }

    @Bean
    public AuthenticationManagerSupplierSelector authenticationManagerSupplierSelector(List<AuthenticationManagerSupplier> suppliers) {
        return new DefaultAuthenticationManagerSupplierSelector(suppliers);
    }

    @Bean
    @ConditionalOnMissingBean
    public StandardAuthenticationProviderProducer standardAuthenticationProviderProducer(UserRepository userRepository,
                                                                                         PasswordEncoder passwordEncoder,
                                                                                         PreAuthenticationChecks preAuthenticationChecks,
                                                                                         PostAuthenticationChecks postAuthenticationChecks) {
        return new StandardAuthenticationProviderProducer(userRepository, passwordEncoder, preAuthenticationChecks, postAuthenticationChecks);
    }

    @Bean
    public CurrentAuthentication currentAuthentication(ObjectProvider<List<AuthenticationResolver>> authenticationResolvers,
                                                       ObjectProvider<List<AuthenticationPrincipalResolver>> authenticationPrincipalResolvers,
                                                       ObjectProvider<List<AuthenticationLocaleResolver>> localeResolvers,
                                                       ObjectProvider<DeviceTimeZoneProvider> deviceTimeZoneProvider) {
        return new DefaultCurrentAuthentication(authenticationResolvers, authenticationPrincipalResolvers, localeResolvers, deviceTimeZoneProvider);
    }

    @Bean
    public SystemAuthenticator systemAuthenticator(AuthenticationManager authenticationManager) {
        return new SystemAuthenticatorImpl(authenticationManager);
    }

    @Bean
    public AuthenticatedAspect authenticatedAspect(SystemAuthenticator systemAuthenticator) {
        return new AuthenticatedAspect(systemAuthenticator);
    }

    @Bean
    public CurrentUserSubstitution currentUserSubstitution(CurrentAuthentication currentAuthentication) {
        return new DefaultCurrentUserSubstitution(currentAuthentication);
    }

    @Bean
    public UserSubstitutionManager userSubstitutionManager(UserRepository userRepository,
                                                           AuthenticationManager authenticationManager,
                                                           CurrentAuthentication currentAuthentication,
                                                           ApplicationEventPublisher eventPublisher,
                                                           Collection<UserSubstitutionProvider> userSubstitutionProviders) {
        return new DefaultUserSubstitutionManager(userRepository, authenticationManager, currentAuthentication, eventPublisher, userSubstitutionProviders);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserSubstitutionProvider userSubstitutionProvider(ApplicationEventPublisher eventPublisher) {
        return new InMemoryUserSubstitutionProvider(eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    protected PersistentTokenRepository persistentTokenRepository() {
        return new InMemoryTokenRepositoryImpl();
    }

    @Bean
    @ConditionalOnMissingBean
    public UserRepository userRepository() {
        return new InMemoryUserRepository();
    }
}
