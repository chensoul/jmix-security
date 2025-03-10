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

package io.jmix.autoconfigure.authserver;

import io.jmix.authserver.AuthServerConfiguration;
import io.jmix.authserver.AuthServerProperties;
import io.jmix.authserver.authentication.OAuth2PasswordTokenEndpointConfigurer;
import io.jmix.authserver.filter.AsResourceServerEventSecurityFilter;
import io.jmix.authserver.introspection.AuthorizationServiceOpaqueTokenIntrospector;
import io.jmix.authserver.introspection.TokenIntrospectorRolesHelper;
import io.jmix.authserver.principal.AuthServerAuthenticationPrincipalResolver;
import io.jmix.authserver.roleassignment.InMemoryRegisteredClientRoleAssignmentRepository;
import io.jmix.authserver.roleassignment.RegisteredClientRoleAssignment;
import io.jmix.authserver.roleassignment.RegisteredClientRoleAssignmentPropertiesMapper;
import io.jmix.authserver.roleassignment.RegisteredClientRoleAssignmentRepository;
import io.jmix.security.JmixSecurityFilterOrder;
import io.jmix.security.util.JmixHttpSecurityUtils;
import io.jmix.securityresourceserver.requestmatcher.CompositeResourceServerRequestMatcherProvider;
import java.util.Collection;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@Import({AuthServerConfiguration.class})
@ConditionalOnProperty(name = "jmix.authserver.use-default-configuration", matchIfMissing = true)
public class AuthServerAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @Order(LoginPageConfiguration.ORDER)
    @ConditionalOnProperty(name = "jmix.authserver.use-default-login-page-configuration", matchIfMissing = true)
    public static class LoginPageConfiguration implements WebMvcConfigurer {

        public static final String SECURITY_CONFIGURER_QUALIFIER = "authorization-server-login-form";

        private final AuthServerProperties authServerProperties;

        public LoginPageConfiguration(AuthServerProperties authServerProperties) {
            this.authServerProperties = authServerProperties;
        }

        public static final int ORDER = 100;

        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController(authServerProperties.getLoginPageUrl())
                    .setViewName(authServerProperties.getLoginPageViewName());
        }

        @Bean("authsr_LoginFormSecurityFilterChain")
        @Order(JmixSecurityFilterOrder.AUTHSERVER_LOGIN_FORM)
        public SecurityFilterChain loginFormSecurityFilterChain(HttpSecurity http)
                throws Exception {
            http
                    .securityMatcher(authServerProperties.getLoginPageUrl(), "/aslogin/styles/**")
                    .authorizeHttpRequests(authorize -> {
                        authorize.anyRequest().permitAll();
                    })
                    .formLogin(form -> {
                        form.loginPage(authServerProperties.getLoginPageUrl());
                    });

            JmixHttpSecurityUtils.applySecurityConfigurersWithQualifier(http, SECURITY_CONFIGURER_QUALIFIER);
            return http.build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "jmix.authserver.use-default-authorization-server-configuration", matchIfMissing = true)
    public static class AuthorizationServerConfiguration {

        public static final String SECURITY_CONFIGURER_QUALIFIER = "authorization-server";
        private final AuthServerProperties authServerProperties;

        public AuthorizationServerConfiguration(AuthServerProperties authServerProperties) {
            this.authServerProperties = authServerProperties;
        }

        /**
         * Enables CORS for pre-flight requests to OAuth2 endpoints
         */
        @Bean("authsr_AuthorizationServerCorsSecurityFilterChain")
        @Order(JmixSecurityFilterOrder.AUTHSERVER_AUTHORIZATION_SERVER + 5)
        public SecurityFilterChain authorizationServerCorsSecurityFilterChain(HttpSecurity http)
                throws Exception {
            http.securityMatcher(antMatcher(HttpMethod.OPTIONS, "/oauth2/**"));
            http.cors(Customizer.withDefaults());
            return http.build();
        }

        @Bean("authsr_AuthorizationServerSecurityFilterChain")
        @Order(JmixSecurityFilterOrder.AUTHSERVER_AUTHORIZATION_SERVER)
        public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
                throws Exception {
            OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
            http
                    // Redirect to the login page when not authenticated from the
                    // authorization endpoint
                    .exceptionHandling((exceptions) -> exceptions
                            .authenticationEntryPoint(
                                    new LoginUrlAuthenticationEntryPoint(authServerProperties.getLoginPageUrl()))
                    )
                    .cors(Customizer.withDefaults());
            http.with(new OAuth2PasswordTokenEndpointConfigurer(), Customizer.withDefaults());
            JmixHttpSecurityUtils.applySecurityConfigurersWithQualifier(http, SECURITY_CONFIGURER_QUALIFIER);
            return http.build();
        }

        @Bean
        @ConditionalOnMissingBean
        public OAuth2AuthorizationService oAuth2AuthorizationService() {
            return new InMemoryOAuth2AuthorizationService();
        }

        @Bean
        public AuthorizationServerSettings authorizationServerSettings() {
            return AuthorizationServerSettings.builder().build();
        }

        @Bean
        @ConditionalOnMissingBean(RegisteredClientRoleAssignmentRepository.class)
        public InMemoryRegisteredClientRoleAssignmentRepository inMemoryRegisteredClientRoleAssignmentRepository() {
            Collection<RegisteredClientRoleAssignment> registeredClientRoleAssignments =
                    new RegisteredClientRoleAssignmentPropertiesMapper(authServerProperties).asRegisteredClientRoleAssignments();
            return new InMemoryRegisteredClientRoleAssignmentRepository(registeredClientRoleAssignments);
        }

        @Bean("authsr_AuthServerAuthenticationPrincipalResolver")
        @Order(100)
        AuthServerAuthenticationPrincipalResolver authServerAuthenticationPrincipalResolver() {
            return new AuthServerAuthenticationPrincipalResolver();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "jmix.authserver.use-default-resource-server-configuration", matchIfMissing = true)
    public static class ResourceServerConfiguration {

        public static final String SECURITY_CONFIGURER_QUALIFIER = "authorization-server-resource-server";

        @Bean("authsr_ResourceServerSecurityFilterChain")
        @Order(JmixSecurityFilterOrder.AUTHSERVER_RESOURCE_SERVER)
        public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http,
                                                                     OpaqueTokenIntrospector opaqueTokenIntrospector,
                                                                     ApplicationEventPublisher applicationEventPublisher,
                                                                     CompositeResourceServerRequestMatcherProvider securityMatcherProvider) throws Exception {
            RequestMatcher authenticatedRequestMatcher = securityMatcherProvider.getAuthenticatedRequestMatcher();
            RequestMatcher anonymousRequestMatcher = securityMatcherProvider.getAnonymousRequestMatcher();
            RequestMatcher securityMatcher = new OrRequestMatcher(authenticatedRequestMatcher, anonymousRequestMatcher);
            http
                    .securityMatcher(securityMatcher)
                    .authorizeHttpRequests(authorize -> {
                        authorize
                                .requestMatchers(anonymousRequestMatcher).permitAll()
                                .requestMatchers(authenticatedRequestMatcher).authenticated();
                    })
                    .oauth2ResourceServer(oauth2 -> oauth2
                            .opaqueToken(opaqueToken -> opaqueToken
                                    .introspector(opaqueTokenIntrospector)))
                    .csrf(csrf -> csrf.disable())
                    .cors(Customizer.withDefaults());

            JmixHttpSecurityUtils.configureAnonymous(http);
            JmixHttpSecurityUtils.configureFrameOptions(http);

            AsResourceServerEventSecurityFilter asResourceServerEventSecurityFilter = new AsResourceServerEventSecurityFilter(applicationEventPublisher);
            http.addFilterAfter(asResourceServerEventSecurityFilter, AuthorizationFilter.class);
            JmixHttpSecurityUtils.applySecurityConfigurersWithQualifier(http, SECURITY_CONFIGURER_QUALIFIER);
            return http.build();
        }

        @ConditionalOnMissingBean
        @Bean("authsr_OpaqueTokenIntrospector")
        public OpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2AuthorizationService authorizationService,
                                                               TokenIntrospectorRolesHelper tokenIntrospectorRolesHelper) {
            return new AuthorizationServiceOpaqueTokenIntrospector(authorizationService, tokenIntrospectorRolesHelper);
        }
    }
}
