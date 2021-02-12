package sk.services.time;

/*-
 * #%L
 * Swiss Knife
 * %%
 * Copyright (C) 2019 Core General
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

import lombok.val;
import sk.utils.paging.RingPicker;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class UtcSettableTimeUtilImpl extends TimeUtcImpl implements ITimeSetter {
    private final AtomicReference<RingPicker<Long>> intSequence = new AtomicReference<>();

    @Override
    public ZonedDateTime nowZ() {
        return toZDT(now());
    }

    @Override
    public long now() {
        val rp = intSequence.get();
        if (rp == null || rp.get().isEmpty()) {
            return toMilli(super.nowZ());
        } else {
            val val = rp.get().get();
            rp.nextStop();
            return val;
        }
    }

    @Override
    public void setCurrentTimeSequence(List<Long> times) {
        if (times == null || times.size() == 0) {
            intSequence.set(null);
        } else {
            intSequence.set(RingPicker.create(times));
        }
    }
}
