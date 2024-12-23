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

package io.jmix.security.authentication.impl;

import com.google.common.base.Strings;
import io.jmix.core.HasTimeZone;
import io.jmix.security.authentication.AuthenticationLocaleResolver;
import io.jmix.security.authentication.AuthenticationPrincipalResolver;
import io.jmix.security.authentication.AuthenticationResolver;
import io.jmix.security.authentication.CurrentAuthentication;
import io.jmix.security.authentication.DeviceTimeZoneProvider;
import io.jmix.security.user.ClientDetails;
import io.jmix.security.util.SecurityContextHelper;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
public class DefaultCurrentAuthentication implements CurrentAuthentication {
    private final ObjectProvider<List<AuthenticationResolver>> authenticationResolvers;
    private final ObjectProvider<List<AuthenticationPrincipalResolver>> authenticationPrincipalResolvers;
    private final ObjectProvider<List<AuthenticationLocaleResolver>> localeResolvers;
    private final ObjectProvider<DeviceTimeZoneProvider> deviceTimeZoneProvider;

    @Override
    public Authentication getAuthentication() {
        Authentication authentication = SecurityContextHelper.getAuthentication();
        if (authentication==null) {
            throw new IllegalStateException("Authentication is not set. " +
                    "Use SystemAuthenticator in non-user requests like schedulers or asynchronous calls.");
        }
        if (CollectionUtils.isNotEmpty(authenticationResolvers.getIfAvailable())) {
            return authenticationResolvers.getIfAvailable().stream()
                    .filter(resolver -> resolver.supports(authentication))
                    .findFirst()
                    .map(resolver -> resolver.resolveAuthentication(authentication))
                    .orElse(authentication);
        }
        return authentication;
    }

    @Override
    public UserDetails getUser() {
        Authentication authentication = getAuthentication();
        Object principal = null;
        if (CollectionUtils.isNotEmpty(authenticationPrincipalResolvers.getIfAvailable())) {
            principal = authenticationPrincipalResolvers.getIfAvailable().stream()
                    .filter(resolver -> resolver.supports(authentication))
                    .findFirst()
                    .map(resolver -> resolver.resolveAuthenticationPrincipal(authentication))
                    .orElse(null);
        }
        if (principal==null) {
            principal = authentication.getPrincipal();
        }
        if (principal instanceof UserDetails) {
            return (UserDetails) principal;
        } else {
            throw new RuntimeException("Authentication principal must be UserDetails");
        }
    }

    @Override
    public Locale getLocale() {
        Authentication authentication = getAuthentication();
        Object details = authentication.getDetails();
        if (details instanceof ClientDetails) {
            Locale locale = ((ClientDetails) details).getLocale();
            if (locale!=null) {
                return locale;
            }
        }

        if (CollectionUtils.isNotEmpty(localeResolvers.getIfAvailable())) {
            for (AuthenticationLocaleResolver resolver : localeResolvers.getIfAvailable()) {
                if (resolver.supports(authentication)) {
                    Locale resolvedLocale = resolver.getLocale(authentication);
                    if (resolvedLocale!=null) {
                        return resolvedLocale;
                    }
                }
            }
        }

        return getDefaultLocale();
    }

    @Override
    public TimeZone getTimeZone() {
        Authentication authentication = getAuthentication();
        Object details = authentication.getDetails();
        Object principal = authentication.getPrincipal();

        TimeZone timeZone = null;

        if (principal instanceof HasTimeZone hasTimeZone) {
            String timeZoneId = hasTimeZone.getTimeZoneId();

            if (!Strings.isNullOrEmpty(timeZoneId)) {
                timeZone = TimeZone.getTimeZone(timeZoneId);
            } else if (hasTimeZone.isAutoTimeZone() && deviceTimeZoneProvider.getIfAvailable()!=null) {
                timeZone = deviceTimeZoneProvider.getIfAvailable().getDeviceTimeZone();
            }
        }

        if (timeZone==null && details instanceof ClientDetails clientDetails) {
            timeZone = clientDetails.getTimeZone();
        }

        return timeZone==null ? TimeZone.getDefault():timeZone;
    }

    @Override
    public boolean isSet() {
        return SecurityContextHelper.getAuthentication()!=null;
    }

    protected Locale getDefaultLocale() {
        return LocaleContextHolder.getLocale();
    }
}
