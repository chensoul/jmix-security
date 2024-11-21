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

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.checkerframework.checker.units.qual.A;
import org.springframework.lang.Nullable;
import java.io.Serializable;

/**
 * The EmailHeader class stores a name/value pair to represent headers.
 */
@Getter
@AllArgsConstructor
public class EmailHeader implements Serializable {

    private static final long serialVersionUID = 2750666832862630139L;
    private static final String SEPARATOR = ":";
    /**
     * The name of the header.
     */
    protected String name;

    /**
     * The value of the header.
     */
    protected String value;

    /**
     * Returns instance of EmailHeader object from String argument.
     *
     * @param line EmailHeader name and value separated with ":" symbol.
     * @return Instance of EmailHeader object. Returns {@code null} if string has wrong format or {@code null} value.
     */
    @Nullable
    public static EmailHeader parse(@Nullable String line) {
        if (line==null)
            return null;
        String[] values = line.split(SEPARATOR);
        if (values.length==2) {
            String name = values[0].trim();
            String value = values[1].trim();
            return new EmailHeader(name, value);
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", this.getName(), SEPARATOR, this.getValue());
    }
}
