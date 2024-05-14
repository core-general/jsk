package sk.utils.statics;

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

import sk.utils.functional.R;

import java.time.*;
import java.time.format.DateTimeFormatter;

@SuppressWarnings({"unused", "WeakerAccess", "SpellCheckingInspection"})
public final class Ti/*mes*/ {
    public static final long second = 1_000L;
    public static final long minute = 60 * second;
    public static final long hour = 60 * minute;
    public static final long day = 24 * hour;
    public static final long week = 7 * day;

    public static final DateTimeFormatter yyyyMMddHHmmssSSS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final DateTimeFormatter yyyyMMddHHmmss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter yyyyMMddHHmm = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter yyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter HHmmssSSS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    public static final DateTimeFormatter HHmmss = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter mmss = DateTimeFormatter.ofPattern("mm:ss");
    public static final DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter HH = DateTimeFormatter.ofPattern("HH");

    public static final ZoneId Moscow = ZoneId.of("Europe/Moscow");
    public static final ZoneId UTC = ZoneOffset.UTC;

    public static final Instant minInstant = Instant.ofEpochMilli(Long.MIN_VALUE);
    public static final Instant maxInstant = Instant.ofEpochMilli(Long.MAX_VALUE);
    public static final ZonedDateTime minZonedDateTime = minInstant.atZone(ZoneOffset.UTC);
    public static final ZonedDateTime maxZonedDateTime = maxInstant.atZone(ZoneOffset.UTC);

    //region Betweens
    public static Duration between(LocalDate startDate, LocalDate endDate) {
        return Ti.between(startDate.atStartOfDay(), endDate.atStartOfDay());
    }

    public static Duration between(LocalDateTime startDate, LocalDateTime endDate) {
        return Duration.between(startDate, endDate);
    }

    public static Duration between(ZonedDateTime startDate, ZonedDateTime endDate) {
        return Duration.between(startDate, endDate);
    }

    public static Duration between(Instant startDate, Instant endDate) {
        return Duration.between(startDate, endDate);
    }

    public static Duration between(long startDate, long endDate) {
        return Duration.between(Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
    }
    //endregion

    public static boolean isSequence(ZonedDateTime... dates) {
        if (dates.length == 0 || dates.length == 1) {
            return true;
        }

        ZonedDateTime curDate = dates[0];
        for (int i = 1; i < dates.length; i++) {
            if (curDate.isAfter(dates[i])) {
                return false;
            }
            curDate = dates[i];
        }
        return true;
    }

    public static double naiveProfile(R r, int count) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            r.run();
        }
        long end = System.currentTimeMillis();
        return ((double) (end - start)) / (count /*to millis*/);
    }

    public static String naiveProfileMS(R r, int count) {
        return String.format("%.5fms", naiveProfile(r, count));
    }

    public static String cronEveryXSeconds(int xSec) {
        return String.format("0/%d * * * * ?", xSec);
    }

    public static String cronEveryXMinutes(int xMin) {
        return String.format("0 0/%d * * * ?", xMin);
    }

    public static String cronEveryDayByHourMinute(int hour, int minutes) {
        if (!(0 <= hour && hour <= 23 ||
                0 <= minutes && minutes <= 59)) {
            throw new RuntimeException("Wrong time period");
        }
        return String.format("0 %d %d * * ?", minutes, hour);
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Ex.thRow(e);
        }
    }

    private Ti() {}
}
