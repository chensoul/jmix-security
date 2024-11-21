/*
 * Copyright 2021 Haulmont.
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

package io.jmix.ldap.search;

import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.ldap.search.LdapUserSearch;

import java.util.Set;

/**
 * Extension of LdapUserSearch interface which adds extra method required by Jmix.
 */
public interface JmixLdapUserSearch extends LdapUserSearch {
    /**
     * Locates multiple user in the directory by the given substring
     * and returns the LDAP information for those users.
     *
     * @param substring the substring of login name supplied to the authentication service.
     * @return a set of DirContextOperations objects containing the user's full DN and requested attributes.
     */
    Set<DirContextOperations> searchForUsersBySubstring(String substring);
}
