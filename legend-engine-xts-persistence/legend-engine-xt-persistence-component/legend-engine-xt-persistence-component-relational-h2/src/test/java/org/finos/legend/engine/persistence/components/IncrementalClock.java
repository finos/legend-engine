// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components;

import java.io.Serializable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/*
A variation of Fixed Clock which increments by a fixed duration every time it's invoked
 */
public class IncrementalClock extends Clock implements Serializable
{

    private Instant instant;
    private final ZoneId zone;

    private final Long millis;

    public IncrementalClock(Instant fixedInstant, ZoneId zone, long millis)
    {
        this.instant = fixedInstant;
        this.zone = zone;
        this.millis = millis;
    }

    @Override
    public ZoneId getZone()
    {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone)
    {
        if (zone.equals(this.zone))
        {
            return this;
        }
        return new IncrementalClock(instant, zone, millis);
    }

    @Override
    public long millis()
    {
        return instant.toEpochMilli();
    }

    @Override
    public Instant instant()
    {
        Instant currentInstant = instant;
        // Increment the instant by given duration
        instant = currentInstant.plusMillis(millis);
        return currentInstant;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof IncrementalClock)
        {
            IncrementalClock other = (IncrementalClock) obj;
            return instant.equals(other.instant) && zone.equals(other.zone) && millis.equals(other.millis);
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return instant.hashCode() ^ zone.hashCode();
    }

    @Override
    public String toString()
    {
        return "FixedClock[" + instant + "," + zone + "," + millis + "]";
    }
}