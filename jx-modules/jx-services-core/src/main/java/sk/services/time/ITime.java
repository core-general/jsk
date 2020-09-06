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

import java.time.*;

@SuppressWarnings("unused")
public interface ITime {
    @SuppressWarnings("SameReturnValue")
    ZoneId getZone();

    default Clock getClock() {
        return Clock.system(getZone());
    }

    default long toMilli(ZonedDateTime zdt) {
        return zdt.toInstant().toEpochMilli();
    }

    default long toSec(ZonedDateTime zdt) {
        return toMilli(zdt) / 1000;
    }

    default ZonedDateTime toZDT(long epochMilli) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), getZone());
    }

    default LocalDate toLD(long epochMilli) {
        return toZDT(epochMilli).toLocalDate();
    }

    default long now() {
        return nowZ().toInstant().toEpochMilli();
    }

    default long nowNano4Dif() {
        return System.nanoTime();
    }

    default Instant nowI() {
        return nowZ().toInstant();
    }

    default LocalDate nowLD() {
        return nowZ().toLocalDate();
    }

    default LocalTime nowLT() {
        return nowZ().toLocalTime();
    }

    default LocalDateTime nowLDT() {
        return nowZ().toLocalDateTime();
    }

    default OffsetDateTime nowO() {
        return nowZ().toOffsetDateTime();
    }

    default ZonedDateTime nowZ() {
        return ZonedDateTime.now(getClock());
    }

    default long getDifWith(ZonedDateTime old) {
        return getDifWithNano4Dif(toMilli(old));
    }

    default long getDifWithNano4Dif(long oldNano4Dif) {
        return nowNano4Dif() - oldNano4Dif;
    }
}
