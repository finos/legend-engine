// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.sql.grammar.from;

import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.JulianFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostgresDateParser
{
    private static final Pattern BC_AD_PATTERN = Pattern.compile("(?i)\\s+(BC|AD|B\\.C\\.|A\\.D\\.)\\s*$");
    private static final Pattern JULIAN_PATTERN = Pattern.compile("^J(\\d+)$");
    private static final Pattern YEAR_DOY_PATTERN = Pattern.compile("^(\\d{4})\\.(\\d{1,3})$");
    private static final Pattern COMPACT_DATE_PATTERN = Pattern.compile("^(\\d{8})$");
    private static final Pattern COMPACT_DATE_SHORT_PATTERN = Pattern.compile("^(\\d{6})$");
    private static final Pattern TZ_ABBREV_PATTERN = Pattern.compile("\\s+(?:UTC|GMT|EST|EDT|CST|CDT|MST|MDT|PST|PDT|CET|CEST|EET|EEST|IST|JST|KST|NZST|NZDT|ACST|AEST|AWST|HST|AKST|AKDT|AST|ADT|NST|NDT)\\s*$");
    private static final Pattern DOT_DMY_PATTERN = Pattern.compile("^\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}(\\s|$)");
    private static final Pattern AMBIGUOUS_NUMERIC_SLASH_PATTERN = Pattern.compile("^\\d{1,2}/\\d{1,2}/\\d{2,4}(\\s|$)");
    private static final Pattern AMBIGUOUS_NUMERIC_DASH_PATTERN = Pattern.compile("^\\d{1,2}-\\d{1,2}-\\d{4}(\\s|$)");
    private static final Pattern SUB_MILLISECOND_PATTERN = Pattern.compile("\\.\\d{4,}");
    private static final Pattern FRACTIONAL_SECONDS_PATTERN = Pattern.compile("\\.(\\d+)");

    private static final String OPTIONAL_SECONDS = "[:ss[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]]";

    private static final List<DateTimeFormatter> TIMESTAMP_FORMATTERS = new ArrayList<>();
    private static final List<DateTimeFormatter> DATE_FORMATTERS = new ArrayList<>();
    private static final List<DateTimeFormatter> OFFSET_FORMATTERS = new ArrayList<>();

    static
    {
        // === Offset timestamp formatters ===
        OFFSET_FORMATTERS.add(new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .appendOffset("+HH:MM:ss", "+00")
                .toFormatter(Locale.ENGLISH));
        OFFSET_FORMATTERS.add(new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .appendOffset("+HH", "+00")
                .toFormatter(Locale.ENGLISH));
        OFFSET_FORMATTERS.add(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // === Timestamp formatters (no offset) ===

        // ISO: 1999-01-08 04:05:06.789 or 1999-01-08T04:05:06
        TIMESTAMP_FORMATTERS.add(new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .toFormatter(Locale.ENGLISH));
        TIMESTAMP_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // AM/PM with ISO date — must be before 24h patterns to avoid TZ stripping eating "PM"
        TIMESTAMP_FORMATTERS.add(new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .appendPattern("h:mm" + OPTIONAL_SECONDS + " a")
                .toFormatter(Locale.ENGLISH));
        TIMESTAMP_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendPattern("M/d/yyyy h:mm" + OPTIONAL_SECONDS + " a")
                .toFormatter(Locale.ENGLISH));
        TIMESTAMP_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendPattern("MMMM d, yyyy h:mm" + OPTIONAL_SECONDS + " a")
                .toFormatter(Locale.ENGLISH));

        // Month name dd, yyyy HH:mm:ss
        addTimestampFormatter("MMMM d, yyyy HH:mm" + OPTIONAL_SECONDS);
        addTimestampFormatter("MMM d, yyyy HH:mm" + OPTIONAL_SECONDS);

        // dd Month yyyy HH:mm:ss
        addTimestampFormatter("d MMMM yyyy HH:mm" + OPTIONAL_SECONDS);
        addTimestampFormatter("d MMM yyyy HH:mm" + OPTIONAL_SECONDS);

        // Postgres-style
        addTimestampFormatter("EEE MMM d yyyy HH:mm" + OPTIONAL_SECONDS);
        addTimestampFormatter("EEE MMM d HH:mm" + OPTIONAL_SECONDS + " yyyy");

        // SQL slash
        addTimestampFormatter("M/d/yyyy HH:mm" + OPTIONAL_SECONDS);

        // German dot
        addTimestampFormatter("d.M.yyyy HH:mm" + OPTIONAL_SECONDS);

        // Mixed
        addTimestampFormatter("yyyy-MMM-d HH:mm" + OPTIONAL_SECONDS);
        addTimestampFormatter("d-MMM-yyyy HH:mm" + OPTIONAL_SECONDS);

        // Month dd yyyy (no comma)
        addTimestampFormatter("MMMM d yyyy HH:mm" + OPTIONAL_SECONDS);

        // Two-digit year ISO-like
        TIMESTAMP_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendValueReduced(ChronoField.YEAR, 2, 2, 1970)
                .appendLiteral('-')
                .appendPattern("M-d HH:mm" + OPTIONAL_SECONDS)
                .toFormatter(Locale.ENGLISH));

        // === Date-only formatters ===
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE);

        // Month name with comma
        addDateFormatter("MMMM d, yyyy");
        addDateFormatter("MMM d, yyyy");

        // Flexible year width for BC dates (1-4 digits)
        DATE_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendPattern("MMMM d, ")
                .appendValue(ChronoField.YEAR, 1, 4, SignStyle.NOT_NEGATIVE)
                .toFormatter(Locale.ENGLISH));

        // dd Month yyyy
        addDateFormatter("d MMMM yyyy");
        addDateFormatter("d MMM yyyy");

        // Month dd yyyy (no comma)
        addDateFormatter("MMMM d yyyy");
        addDateFormatter("MMM d yyyy");

        // Postgres-style
        addDateFormatter("EEE MMM d yyyy");

        // SQL slash
        addDateFormatter("M/d/yyyy");

        // German dot
        addDateFormatter("d.M.yyyy");

        // Mixed with month abbreviation
        addDateFormatter("yyyy-MMM-d");
        addDateFormatter("d-MMM-yyyy");
        addDateFormatter("MMM-d-yyyy");

        // yyyy/M/d
        addDateFormatter("yyyy/M/d");

        // d/M/yyyy
        addDateFormatter("d/M/yyyy");

        // M-d-yyyy
        addDateFormatter("M-d-yyyy");

        // Two-digit year
        DATE_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendValueReduced(ChronoField.YEAR, 2, 2, 1970)
                .appendLiteral('-')
                .appendPattern("M-d")
                .toFormatter(Locale.ENGLISH));
        DATE_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendPattern("M/d/")
                .appendValueReduced(ChronoField.YEAR, 2, 2, 1970)
                .toFormatter(Locale.ENGLISH));
        DATE_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendPattern("d-MMM-")
                .appendValueReduced(ChronoField.YEAR, 2, 2, 1970)
                .toFormatter(Locale.ENGLISH));
    }

    private static void addTimestampFormatter(String pattern)
    {
        TIMESTAMP_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH));
    }

    private static void addDateFormatter(String pattern)
    {
        DATE_FORMATTERS.add(new DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .toFormatter(Locale.ENGLISH));
    }

    private PostgresDateParser()
    {
    }

    public static PureDate parse(String dateString)
    {
        if (dateString == null || dateString.trim().isEmpty())
        {
            throw new IllegalArgumentException("Cannot parse empty date string");
        }

        String trimmed = dateString.trim();

        // Handle special values
        PureDate special = tryParseSpecialValue(trimmed);
        if (special != null)
        {
            return special;
        }

        // Dot-separated D.M.YYYY is ambiguous without knowing Postgres DateStyle (MDY vs DMY)
        if (DOT_DMY_PATTERN.matcher(trimmed).find())
        {
            throw new UnsupportedOperationException("Dot-separated date format (e.g. '8.1.1999') is ambiguous without Postgres DateStyle context and is not supported");
        }

        // Slash-separated M/D/YYYY is ambiguous without knowing Postgres DateStyle (MDY vs DMY)
        if (AMBIGUOUS_NUMERIC_SLASH_PATTERN.matcher(trimmed).find())
        {
            throw new UnsupportedOperationException("Numeric slash-separated date format (e.g. '1/8/1999') is ambiguous without Postgres DateStyle context and is not supported");
        }

        // Dash-separated M-D-YYYY is ambiguous without knowing Postgres DateStyle (MDY vs DMY)
        if (AMBIGUOUS_NUMERIC_DASH_PATTERN.matcher(trimmed).find())
        {
            throw new UnsupportedOperationException("Numeric dash-separated date format (e.g. '1-8-1999') is ambiguous without Postgres DateStyle context and is not supported");
        }

//        // Sub-millisecond precision (>3 fractional digits) is not supported — downstream stores (e.g. H2) truncate to milliseconds
//        if (SUB_MILLISECOND_PATTERN.matcher(trimmed).find())
//        {
//            throw new UnsupportedOperationException("Sub-millisecond precision (more than 3 fractional digits) is not supported");
//        }

        // Strip BC/AD suffix
        boolean isBc = false;
        Matcher bcMatcher = BC_AD_PATTERN.matcher(trimmed);
        if (bcMatcher.find())
        {
            String era = bcMatcher.group(1).toUpperCase().replace(".", "");
            isBc = "BC".equals(era);
            trimmed = trimmed.substring(0, bcMatcher.start()).trim();
        }

        // Julian day number: J2451187
        Matcher julianMatcher = JULIAN_PATTERN.matcher(trimmed);
        if (julianMatcher.matches())
        {
            long julianDay = Long.parseLong(julianMatcher.group(1));
            LocalDate date = LocalDate.MIN.with(JulianFields.JULIAN_DAY, julianDay);
            return adjustEra(toPureDate(date), isBc, date.getYear());
        }

        // Year.DayOfYear: 1999.008
        Matcher doyMatcher = YEAR_DOY_PATTERN.matcher(trimmed);
        if (doyMatcher.matches())
        {
            int year = Integer.parseInt(doyMatcher.group(1));
            int doy = Integer.parseInt(doyMatcher.group(2));
            LocalDate date = LocalDate.ofYearDay(year, doy);
            return adjustEra(toPureDate(date), isBc, date.getYear());
        }

        // Compact yyyyMMdd: 19990108
        Matcher compactMatcher = COMPACT_DATE_PATTERN.matcher(trimmed);
        if (compactMatcher.matches())
        {
            LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.BASIC_ISO_DATE);
            return adjustEra(toPureDate(date), isBc, date.getYear());
        }

        // Compact yyMMdd: 990108
        Matcher compactShortMatcher = COMPACT_DATE_SHORT_PATTERN.matcher(trimmed);
        if (compactShortMatcher.matches())
        {
            int yy = Integer.parseInt(trimmed.substring(0, 2));
            int mm = Integer.parseInt(trimmed.substring(2, 4));
            int dd = Integer.parseInt(trimmed.substring(4, 6));
            int year = yy >= 70 ? 1900 + yy : 2000 + yy;
            LocalDate date = LocalDate.of(year, mm, dd);
            return adjustEra(toPureDate(date), isBc, year);
        }

        // Normalize multiple spaces to single
        String normalized = trimmed.replaceAll("\\s+", " ");

        // Strip timezone abbreviations (e.g., PST, UTC) — but not AM/PM
        normalized = TZ_ABBREV_PATTERN.matcher(normalized).replaceFirst("");

        // Try offset (timezone-aware) formatters first
        for (DateTimeFormatter fmt : OFFSET_FORMATTERS)
        {
            try
            {
                OffsetDateTime odt = OffsetDateTime.parse(normalized, fmt);
                LocalDateTime utc = odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
                return adjustEra(toPureDateTime(utc, extractSubseconds(normalized)), isBc, utc.getYear());
            }
            catch (DateTimeParseException ignored)
            {
                // try next
            }
        }

        // Try timestamp formatters
        for (DateTimeFormatter fmt : TIMESTAMP_FORMATTERS)
        {
            try
            {
                LocalDateTime ldt = LocalDateTime.parse(normalized, fmt);
                return adjustEra(toPureDateTime(ldt, extractSubseconds(normalized)), isBc, ldt.getYear());
            }
            catch (DateTimeParseException ignored)
            {
                // try next
            }
        }

        // Try date-only formatters
        for (DateTimeFormatter fmt : DATE_FORMATTERS)
        {
            try
            {
                LocalDate ld = LocalDate.parse(normalized, fmt);
                return adjustEra(toPureDate(ld), isBc, ld.getYear());
            }
            catch (DateTimeParseException ignored)
            {
                // try next
            }
        }

        throw new IllegalArgumentException("Failed to parse date string: " + dateString);
    }

    private static PureDate tryParseSpecialValue(String value)
    {
        switch (value.toLowerCase(Locale.ENGLISH))
        {
            case "epoch":
                return toPureDateTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            case "infinity":
            case "-infinity":
                throw new IllegalArgumentException("Infinity dates are not supported in Pure");
            case "now":
                return toPureDateTime(LocalDateTime.now());
            case "today":
                return toPureDate(LocalDate.now());
            case "tomorrow":
                return toPureDate(LocalDate.now().plusDays(1));
            case "yesterday":
                return toPureDate(LocalDate.now().minusDays(1));
            default:
                return null;
        }
    }

    private static PureDate toPureDate(LocalDate ld)
    {
        return DateFunctions.newPureDate(ld.getYear(), ld.getMonthValue(), ld.getDayOfMonth());
    }

    private static PureDate toPureDateTime(LocalDateTime ldt)
    {
        return toPureDateTime(ldt, null);
    }

    private static PureDate toPureDateTime(LocalDateTime ldt, String originalSubseconds)
    {
        if (originalSubseconds != null)
        {
            return DateFunctions.newPureDate(ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
                    ldt.getHour(), ldt.getMinute(), ldt.getSecond(), originalSubseconds);
        }
        if (ldt.getNano() != 0)
        {
            String subsecond = String.format("%09d", ldt.getNano());
            return DateFunctions.newPureDate(ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
                    ldt.getHour(), ldt.getMinute(), ldt.getSecond(), subsecond);
        }
        return DateFunctions.newPureDate(ldt.getYear(), ldt.getMonthValue(), ldt.getDayOfMonth(),
                ldt.getHour(), ldt.getMinute(), ldt.getSecond());
    }

    private static String extractSubseconds(String input)
    {
        Matcher m = FRACTIONAL_SECONDS_PATTERN.matcher(input);
        if (m.find())
        {
            return m.group(1);
        }
        return null;
    }

    private static PureDate adjustEra(PureDate date, boolean isBc, int year)
    {
        if (isBc)
        {
            int bcYear = -(year - 1);
            String dateStr = date.toString();
            String adjustedStr = dateStr.replaceFirst("^\\d+", String.valueOf(bcYear));
            return DateFunctions.parsePureDate(adjustedStr);
        }
        return date;
    }
}
