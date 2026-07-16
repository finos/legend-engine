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
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlanDateParameterDateFormat extends TemplateDateFormat
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PlanDateParameterDateFormat.class);

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
        private final String basePattern;

        PlanDateParameterFormatter(String datePattern, boolean isDateTime, boolean hasTimeOffset, boolean performTzConversion, boolean hasSubSecond)
        {
            this.isDateTime = isDateTime;
            this.hasTimeOffset = hasTimeOffset;
            this.hasSubSecond = hasSubSecond;
            this.datePattern = datePattern;
            this.basePattern = hasSubSecond ? datePattern.substring(0, datePattern.indexOf(".SSS")) : datePattern;
            this.dateFormatter = hasSubSecond ? buildSubSecondFormatter(this.basePattern, hasTimeOffset) : DateTimeFormatter.ofPattern(datePattern);
            this.performTzConversion = performTzConversion;
        }

        private static DateTimeFormatter buildSubSecondFormatter(String basePattern, boolean hasTimeOffset)
        {
            // Accept 1-9 fractional-second digits (milliseconds, microseconds, nanoseconds) rather than the
            // fixed 3-digit '.SSS' pattern, which rejects e.g. microsecond precision such as '.000000'.
            DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                    .appendPattern(basePattern)
                    .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true);
            if (hasTimeOffset)
            {
                builder.appendPattern("Z");
            }
            return builder.toFormatter();
        }

        /**
         * Builds an output formatter that preserves the fractional-second precision of the original date string.
         * For example, if the input had 6 fractional digits (".000000"), the output formatter will always emit
         * exactly 6 fractional digits, rather than trimming trailing zeros down to 1 digit.
         */
        DateTimeFormatter buildOutputFormatter(String originalDate)
        {
            int dotIndex = originalDate.indexOf('.');
            if (dotIndex < 0)
            {
                return this.dateFormatter;
            }
            int fracDigits = 0;
            for (int i = dotIndex + 1; i < originalDate.length() && Character.isDigit(originalDate.charAt(i)); i++)
            {
                fracDigits++;
            }
            if (fracDigits == 0)
            {
                return this.dateFormatter;
            }
            int clampedFracDigits = Math.min(Math.max(fracDigits, 1), 9);
            DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                    .appendPattern(this.basePattern)
                    .appendFraction(ChronoField.NANO_OF_SECOND, clampedFracDigits, clampedFracDigits, true);
            if (this.hasTimeOffset)
            {
                builder.appendPattern("Z");
            }
            return builder.toFormatter();
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
        LOGGER.error(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.JSON_PARSING_ERROR, exceptionMessage).toString());
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
                DateTimeFormatter outputFormatter = dtf.hasSubSecond
                        ? dtf.buildOutputFormatter(dateAndTimeZone.date)
                        : dtf.dateFormatter;
                if (dtf.performTzConversion)
                {
                    return new PlanDateParameter(dateTime, outputFormatter, dateAndTimeZone.tz);
                }
                else
                {
                    return new PlanDateParameter(dateTime, outputFormatter);
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