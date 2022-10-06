package sk.si;/*-
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

public record JxGeneralRAMData(long memoryFree, long memoryBusy, long memoryOverall, double percentFree, double percentBusy) {
    public JxGeneralRAMData(long memoryFree, long memoryOverall) {
        this(memoryFree, memoryOverall - memoryFree, memoryOverall, ((double) memoryFree) / memoryOverall,
                ((double) memoryOverall - memoryFree) / memoryOverall);
    }

    @Override
    public String toString() {
        final String[] measurements = {"Kb", "Mb", "Gb", "Tb", "Qb", "Ib"};
        return """
                %s Memory overall
                %s (%.1f%%) Free
                %s (%.1f%%) Busy"""
                .formatted(
                        St.shortNumberForm(memoryOverall, measurements),
                        St.shortNumberForm(memoryFree, measurements), 100 * percentFree,
                        St.shortNumberForm(memoryBusy, measurements), 100 * percentBusy
                );
    }
}
