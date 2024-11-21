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

package io.jmix.security.authentication.provider;

import io.jmix.security.check.PostAuthenticationChecks;
import io.jmix.security.check.PreAuthenticationChecks;
import io.jmix.security.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Class returns a collection of "standard" providers that may be used in AuthenticationManagers created in different
 * security configurations.
 */
@RequiredArgsConstructor
public class StandardAuthenticationProviderProducer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PreAuthenticationChecks preAuthenticationChecks;
    private final PostAuthenticationChecks postAuthenticationChecks;

    public List<AuthenticationProvider> getAuthenticationProviders() {
        List<AuthenticationProvider> providers = new ArrayList<>();
        //系统用户登录
        providers.add(new SystemAuthenticationProvider(userRepository));

        //模拟其他用户登录
        providers.add(new SubstitutedUserAuthenticationProvider(userRepository));

        //普通用户常规登录
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userRepository);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        daoAuthenticationProvider.setPreAuthenticationChecks(preAuthenticationChecks);
        daoAuthenticationProvider.setPostAuthenticationChecks(postAuthenticationChecks);

        providers.add(daoAuthenticationProvider);
        return providers;
    }

}
