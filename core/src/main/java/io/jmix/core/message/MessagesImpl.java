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

package io.jmix.core.message;

import com.google.common.base.Strings;
import static io.jmix.core.util.Preconditions.checkNotNullArgument;
import java.util.IllegalFormatException;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component("core_Messages")
public class MessagesImpl implements Messages {

    @Autowired
    protected MessageSource messageSource;

    @Override
    public String getMessage(String key) {
        return getMessage(key, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage(String key, Locale locale) {
        checkNotNullArgument(key, "key is null");
        checkNotNullArgument(locale, "locale is null");
        try {
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException e) {
            return fallbackMessageOrKey(null, key, locale);
        }
    }

    @Override
    public String getMessage(Class caller, String key) {
        return getMessage(caller, key, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage(Class caller, String key, Locale locale) {
        checkNotNullArgument(caller, "caller is null");
        checkNotNullArgument(key, "key is null");
        checkNotNullArgument(locale, "locale is null");
        try {
            return messageSource.getMessage(getCode(getGroup(caller), key), null, locale);
        } catch (NoSuchMessageException e) {
            return fallbackMessageOrKey(getGroup(caller), key, locale);
        }
    }

    @Override
    public String getMessage(Enum caller) {
        return getMessage(caller, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage(Enum caller, Locale locale) {
        checkNotNullArgument(caller, "caller is null");
        checkNotNullArgument(locale, "locale is null");

        String declaringClassName = caller.getDeclaringClass().getName();
        int i = declaringClassName.lastIndexOf('.');
        if (i > -1)
            declaringClassName = declaringClassName.substring(i + 1);

        return getMessage(
                getGroup(caller.getClass()),
                declaringClassName + "." + caller.name(),
                locale
        );
    }

    @Override
    public String getMessage(String group, String key) {
        return getMessage(group, key, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage(String group, String key, Locale locale) {
        checkNotNullArgument(group, "group is null");
        checkNotNullArgument(key, "key is null");
        checkNotNullArgument(locale, "locale is null");
        try {
            return messageSource.getMessage(getCode(group, key), null, locale);
        } catch (NoSuchMessageException e) {
            return fallbackMessageOrKey(group, key, locale);
        }
    }

    @Override
    public String formatMessage(Class caller, String key, Object... params) {
        try {
            return String.format(getMessage(caller, key), params);
        } catch (IllegalFormatException e) {
            return key;
        }
    }

    @Override
    public String formatMessage(Class caller, String key, Locale locale, Object... params) {
        try {
            return String.format(getMessage(caller, key, locale), params);
        } catch (IllegalFormatException e) {
            return key;
        }
    }

    @Override
    public String formatMessage(String group, String key, Object... params) {
        try {
            return String.format(getMessage(group, key), params);
        } catch (IllegalFormatException e) {
            return key;
        }
    }

    @Override
    public String formatMessage(String group, String key, Locale locale, Object... params) {
        try {
            return String.format(getMessage(group, key, locale), params);
        } catch (IllegalFormatException e) {
            return key;
        }
    }

    @Nullable
    @Override
    public String findMessage(String key, @Nullable Locale locale) {
        checkNotNullArgument(key, "key is null");

        if (locale==null)
            locale = LocaleContextHolder.getLocale();

        try {
            return messageSource.getMessage(key, null, locale);
        } catch (NoSuchMessageException e) {
            return fallbackMessageOrNull(null, key, locale);
        }
    }

    @Nullable
    @Override
    public String findMessage(String group, String key, @Nullable Locale locale) {
        checkNotNullArgument(group, "group is null");
        checkNotNullArgument(key, "key is null");

        if (locale==null)
            locale = LocaleContextHolder.getLocale();

        try {
            return messageSource.getMessage(getCode(group, key), null, locale);
        } catch (NoSuchMessageException e) {
            return fallbackMessageOrNull(group, key, locale);
        }
    }

    @Override
    public void clearCache() {
        if (messageSource instanceof ReloadableResourceBundleMessageSource) {
            ((ReloadableResourceBundleMessageSource) messageSource).clearCache();
        }
    }

    protected String getCode(String group, String key) {
        if (Strings.isNullOrEmpty(group)) {
            return key;
        } else {
            return group + "/" + key;
        }
    }

    protected String getGroup(Class c) {
        String className = c.getName();
        int pos = className.lastIndexOf(".");
        if (pos > 0)
            return className.substring(0, pos);
        else
            return "";
    }

    protected String fallbackMessageOrKey(@Nullable String group, String key, Locale locale) {
        return key;
    }

    @Nullable
    protected String fallbackMessageOrNull(@Nullable String group, String key, Locale locale) {
        return null;
    }
}
