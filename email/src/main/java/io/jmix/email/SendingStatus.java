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

package io.jmix.email;

import io.jmix.core.util.EnumClass;
import org.springframework.lang.Nullable;

public enum SendingStatus implements EnumClass<Integer> {
    QUEUE(0),
    SENDING(100),
    SENT(200),
    NOT_SENT(300);

    private Integer id;

    SendingStatus(Integer id) {
        this.id = id;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Nullable
    public static SendingStatus fromId(Integer id) {
        for (SendingStatus ss : SendingStatus.values()) {
            if (id.equals(ss.getId())) {
                return ss;
            }
        }
        return null;
    }
}