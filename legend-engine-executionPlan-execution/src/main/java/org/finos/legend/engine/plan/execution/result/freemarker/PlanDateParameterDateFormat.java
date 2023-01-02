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

import freemarker.core.TemplateDateFormat;
import freemarker.template.TemplateDateModel;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanDateParameterDateFormat extends TemplateDateFormat
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    private static final RichIterable<PlanDateParameterFormatter> planStrictDateFormatters = Lists.immutable.with(new PlanDateParameterFormatter("yyyy-MM-dd", false, false, false, false));
    private static final RichIterable<PlanDateParameterFormatter> planDateTimeFormatters = Lists.immutable.with(
            new PlanDateParameterFormatter("yyyy-MM-dd'T'HH:mm:ss", true, false, true, false),
            new PlanDateParameterFormatter("yyyy-MM-dd'T'HH:mm:ss.SSS", true, false, true, true),
            new PlanDateParameterFormatter("yyyy-MM-dd HH:mm:ss.SSS", true, false, true, true),
            new PlanDateParameterFormatter("yyyy-MM-dd HH:mm:ss", true, false, true, false),
            new PlanDateParameterFormatter("yyyy-MM-dd'T'HH:mm:ss.SSSZ", true, true, true, true),
            new PlanDateParameterFormatter("yyyy-MM-dd'T'HH:mm:ssZ", true, true, true, false));
    private static final RichIterable<PlanDateParameterFormatter> planDateFormatters = Lists.mutable.withAll(planStrictDateFormatters).withAll(planDateTimeFormatters);

    private static final String dateRegex = "\\[(\\S+)\\]\\s(\\S+([\\sT]\\S+)?)";
    private static final Pattern datePattern = Pattern.compile(dateRegex);
    static final PlanDateParameterDateFormat INSTANCE = new PlanDateParameterDateFormat();

    public static class PlanDateParameterFormatter
    {
        DateTimeFormatter dateFormatter;
        public boolean isDateTime;
        private boolean hasTimeOffset;
        boolean performTzConversion;
        public boolean hasSubSecond;
        private final String datePattern;

        PlanDateParameterFormatter(String datePattern, boolean isDateTime, boolean hasTimeOffset, boolean performTzConversion, boolean hasSubSecond)
        {
            this.isDateTime = isDateTime;
            this.hasTimeOffset = hasTimeOffset;
            this.dateFormatter = DateTimeFormatter.ofPattern(datePattern);
            this.performTzConversion = performTzConversion;
            this.hasSubSecond = hasSubSecond;
            this.datePattern = datePattern;
        }

        public LocalDateTime parse(String date) throws DateTimeParseException
        {
            LocalDateTime localDateTime;
            if (this.hasTimeOffset)
            {
                localDateTime = OffsetDateTime.parse(date, this.dateFormatter).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            }
            else if (this.isDateTime)
            {
                localDateTime = LocalDateTime.parse(date, this.dateFormatter);
            }
            else
            {
                localDateTime = LocalDate.parse(date, this.dateFormatter).atStartOfDay();
            }
            return localDateTime;
        }

        public String getDatePattern()
        {
            return this.datePattern;
        }
    }

    private PlanDateParameterDateFormat()
    {
    }

    public static RichIterable<PlanDateParameterFormatter> getPlanDateFormatters()
    {
        return planDateFormatters;
    }

    public static RichIterable<PlanDateParameterFormatter> getPlanDateFormatters(boolean isDateTime)
    {
        return isDateTime ? planDateTimeFormatters : planStrictDateFormatters;
    }

    @Override
    public java.lang.Object format(freemarker.template.TemplateDateModel dateModel)
    {
        PlanDateParameter pureFreemarkerDate = (PlanDateParameter) dateModel;
        return pureFreemarkerDate.formattedDate();
    }

    @Override
    public PlanDateParameter parse(String dateExpression, int i)
    {
        PlanDateParameter pureFreemarkerDate = getPlanFreemarkerDateTimeForTz(dateExpression);

        if (pureFreemarkerDate == null)
        {
            throwIllegalArgumentException(dateExpression);
        }

        return pureFreemarkerDate;
    }

    private static void throwIllegalArgumentException(String dateExpression) throws IllegalArgumentException
    {
        String exceptionMessage = "Plan parsing error; unable to process Date: " + dateExpression + ", expecting: " + dateRegex + " e.g.: '[EST] + $date', where $date is of format: " + planDateFormatters.collect(pdf -> pdf.datePattern).makeString("[", ", ", "]") + " , e.g. : [EST] 2018-10-15T20:00:00.123";
        LOGGER.error(new LogInfo((String)null, LoggingEventType.JSON_PARSING_ERROR, exceptionMessage).toString());
        throw new IllegalArgumentException(exceptionMessage);
    }

    private PlanDateParameter getPlanFreemarkerDateTimeForTz(String dateExpression)
    {
        DateAndTimeZone dateAndTimeZone = extractDateAndTimeZone(dateExpression);
        return planDateFormatters.asLazy().collect(dtf ->
        {
            try
            {
                LocalDateTime dateTime = dtf.parse(dateAndTimeZone.date);
                if (dtf.performTzConversion)
                {
                    return new PlanDateParameter(dateTime, dtf.dateFormatter, dateAndTimeZone.tz);
                }
                else
                {
                    return new PlanDateParameter(dateTime, dtf.dateFormatter);
                }
            }
            catch (DateTimeParseException e)
            {
                return null;
            }
        }).detect(Predicates.notNull());
    }

    private DateAndTimeZone extractDateAndTimeZone(String dateExpression)
    {
        Matcher m = datePattern.matcher(dateExpression);
        if (!m.matches())
        {
            throwIllegalArgumentException(dateExpression);
        }
        String tz = m.group(1);
        String date = m.group(2);
        return new DateAndTimeZone(date, tz);
    }

    private static class DateAndTimeZone
    {
        String date;
        String tz;

        DateAndTimeZone(String date, String tz)
        {
            this.date = date;
            this.tz = tz;
        }
    }

    @Override
    public String formatToPlainText(TemplateDateModel templateDateModel)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLocaleBound()
    {
        return true;
    }

    @Override
    public boolean isTimeZoneBound()
    {
        return true;
    }

    @Override
    public String getDescription()
    {
        return "Plan Date Format";
    }
}