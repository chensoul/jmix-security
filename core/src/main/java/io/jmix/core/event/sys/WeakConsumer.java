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

package io.jmix.core.event.sys;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

public class WeakConsumer<T> implements Consumer<T> {

    private final WeakReference<Consumer<T>> reference;

    public WeakConsumer(Consumer<T> consumer) {
        reference = new WeakReference<>(consumer);
    }

    @Override
    public void accept(T t) {
        Consumer<T> consumer = reference.get();
        if (consumer != null) {
            consumer.accept(t);
        }
    }
}