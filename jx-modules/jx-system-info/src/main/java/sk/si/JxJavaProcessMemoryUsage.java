package sk.si;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 - 2022 Core General
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

import sk.utils.statics.St;

public record JxJavaProcessMemoryUsage(long heapUsed, long nonHeapUsed, long maxHeap, long maxNonHeap) {

    public long overallUsed() {
        return heapUsed + nonHeapUsed;
    }

    @Override
    public String toString() {
        return "%s heap (%s max); %s nonHeap (%s max)".formatted(
                St.shortNumberForm(heapUsed, St.MEMORY_MEASUREMENTS),
                St.shortNumberForm(maxHeap, St.MEMORY_MEASUREMENTS),

                St.shortNumberForm(nonHeapUsed, St.MEMORY_MEASUREMENTS),
                St.shortNumberForm(maxNonHeap, St.MEMORY_MEASUREMENTS)
        );
    }
}
