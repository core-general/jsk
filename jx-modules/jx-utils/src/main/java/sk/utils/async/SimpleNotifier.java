package sk.utils.async;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2021 Core General
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import lombok.RequiredArgsConstructor;
import sk.utils.functional.C1;

@RequiredArgsConstructor
public class SimpleNotifier {
    private long al = 0;
    final private int maxValue;
    final private long divider;
    final private C1<String> notifier;

    public void incrementAndNotify() {
        final long cur = al++;
        if (cur % divider == 0) {
            notifier.accept(cur + "/" + maxValue);
        }
    }

    public long getCurrentValue() {
        return al;
    }
}
