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

package io.jmix.security.util;

import io.jmix.security.logging.LogMdc;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper class to get/set Authentication in the current {@link SecurityContext}.
 */
public class SecurityContextHelper {
    /**
     * Returns current Authentication or null if the current context has no Authentication
     */
    @Nullable
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Sets the Authentication in the current {@link SecurityContext}.
     */
    public static void setAuthentication(@Nullable Authentication authentication) {
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            LogMdc.setup(authentication);
        } else {
            SecurityContextHolder.clearContext();
            LogMdc.setup(null);
        }
    }
}
