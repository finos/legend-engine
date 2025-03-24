// Copyright 2025 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.runtime.java.extension.functions.standard.shared.natives.date.operation;

import org.finos.legend.pure.m4.coreinstance.primitive.date.DateTime;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateWithSubsecond;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;


public class TimeBucketShared
{
    public static DateTime time_bucket(DateTime dateTime, long quantity, String unit) throws IllegalArgumentException
    {
        if (quantity < 1)
        {
            throw new IllegalArgumentException("Unsupported duration quantity: " + quantity);
        }
        // date math in java.time requires TZ so adding back the UTC
        ZonedDateTime origin = Instant.ofEpochMilli(0).atZone(ZoneId.of("UTC"));
        // avoid dependency on older gregorianCalendar methods
        ZonedDateTime dateTimeTz = dateTime.getCalendar().toZonedDateTime().toInstant().atZone(ZoneId.of("UTC"));
        if (unit.equals("WEEKS"))
        {
            // Gets the ISO week start (if the weekday of a date is element of [Mon, Thu] it belongs to prior Monday)
            // note: below is workaround for ta JDK bug - https://bugs.openjdk.org/browse/JDK-8033662
            // instead of - ZonedDateTime.from(origin.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)).toInstant())
            origin = origin.toLocalDateTime().atZone(ZoneId.of("UTC")).with(TemporalAdjusters.previous(DayOfWeek.MONDAY)).toLocalDateTime().atZone(ZoneId.of("UTC"));
        }

        long bucketWidth;
        boolean milliUnits;
        switch (unit)
        {
            case "YEARS":
            {
                ZonedDateTime originPlusBucketWidth = origin.plus(Period.ofYears((int) quantity));
                // epoch months
                bucketWidth = (originPlusBucketWidth.getYear() - 1970) * 12L + originPlusBucketWidth.getMonth().getValue() - 1;
                milliUnits = false;
                break;
            }
            case "MONTHS":
            {
                bucketWidth = quantity;
                milliUnits = false;
                break;
            }
            case "WEEKS":
            {
                bucketWidth = Math.subtractExact(origin.plus(Period.ofWeeks((int) quantity)).toInstant().toEpochMilli(), origin.toInstant().toEpochMilli());
                milliUnits = true;
                break;
            }
            case "DAYS":
            {
                bucketWidth = Math.subtractExact(origin.plus(Period.ofDays((int) quantity)).toInstant().toEpochMilli(), origin.toInstant().toEpochMilli());
                milliUnits = true;
                break;
            }
            case "HOURS":
            {
                bucketWidth = Duration.ofHours(quantity).toMillis();
                milliUnits = true;
                break;
            }
            case "MINUTES":
            {
                bucketWidth = Duration.ofMinutes(quantity).toMillis();
                milliUnits = true;
                break;
            }
            case "SECONDS":
            {
                bucketWidth = Duration.ofSeconds(quantity).toMillis();
                milliUnits = true;
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Unsupported duration unit: " + unit);
            }
        }

        long res;
        long diff;
        long delta;
        if (milliUnits)
        {
            long dtetime = dateTimeTz.toInstant().toEpochMilli();
            diff = Math.subtractExact(dtetime, origin.toInstant().toEpochMilli());
        }
        else
        {
            // uses epoch months calculation - 1970 is considered as month 0 which belongs to the first positive bucket
            diff = (dateTimeTz.getYear() - 1970) * 12L + dateTimeTz.getMonth().getValue() - 1;
        }

        long mod = diff % bucketWidth;
        delta = Math.subtractExact(diff, mod);
        if (milliUnits)
        {
            res = Math.addExact(origin.toInstant().toEpochMilli(), delta);
            if (mod < 0)
            {
                res = Math.subtractExact(res, bucketWidth);
            }
        }
        else
        {
            ZonedDateTime myres = origin.plus(Period.ofMonths((int) delta));
            if (mod < 0)
            {
                myres = myres.minus(Period.ofMonths((int) bucketWidth));
            }
            res = myres.with(TemporalAdjusters.firstDayOfMonth()).toInstant().toEpochMilli();
        }

        return DateWithSubsecond.fromInstant(Instant.ofEpochMilli(res), 9);
    }
}


