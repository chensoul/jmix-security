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

package io.jmix.security.user.event;

import java.util.Map;

/**
 * Class for user password reset event. It includes user names and new passwords.
 */
public class UserPasswordResetEvent extends UserPasswordChangedEvent {
    private static final long serialVersionUID = -5109995742911792935L;

    public UserPasswordResetEvent(Map<String, String> passwordByUser) {
        super(passwordByUser);
    }
}
