// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.result.freemarker;

import freemarker.template.TemplateDateModel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

class PlanDateParameter implements freemarker.template.TemplateDateModel
{
    private static final TimeZone GMT_TIME_ZONE = TimeZone.getTimeZone("GMT");
    private static final ZoneId gmtZoneId = GMT_TIME_ZONE.toZoneId();
    private final Date processedDate;
    private final String formattedDate;

    public PlanDateParameter(LocalDateTime dateTime, DateTimeFormatter dateTimeFormatter)
    {
        this(dateTime, dateTimeFormatter, PlanDateParameter.gmtZoneId.getId());
    }

    public PlanDateParameter(LocalDateTime date, DateTimeFormatter dateTimeFormatter, String targetTz)
    {
        LocalDateTime dateTimeAdjustedForTargetTz = getTargetZonedDateTime(date, targetTz);
        formattedDate = dateTimeAdjustedForTargetTz.format(dateTimeFormatter);
        processedDate = Date.from(dateTimeAdjustedForTargetTz.atZone(ZoneId.of(targetTz)).toInstant());
    }

    private LocalDateTime getTargetZonedDateTime(LocalDateTime dateTime, String targetTz)
    {
        LocalDateTime dateTimeInTargetZone;
        if (GMT_TIME_ZONE.getID().equals(targetTz) || "UTC".equals(targetTz))
        {
            dateTimeInTargetZone = dateTime;
        }
        else
        {
            ZonedDateTime dateTimeGMT = ZonedDateTime.of(dateTime, gmtZoneId);
            dateTimeInTargetZone = dateTimeGMT.withZoneSameInstant(ZoneId.of(targetTz)).toLocalDateTime();
        }
        return dateTimeInTargetZone;
    }

    @Override
    public Date getAsDate()
    {
        return processedDate;
    }

    @Override
    public int getDateType()
    {
        return TemplateDateModel.DATE;
    }

    public String formattedDate()
    {
        return formattedDate;
    }
}
