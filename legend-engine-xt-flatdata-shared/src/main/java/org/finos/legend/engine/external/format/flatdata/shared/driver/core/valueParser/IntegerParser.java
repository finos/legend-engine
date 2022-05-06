// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public abstract class IntegerParser implements ValueParser
{
    private static final Predicate<String> DEFAULT_IS_VALID_INTEGER = Pattern.compile("[+-]?\\d+").asPredicate();
    private static final Predicate<String> VALID_FORMAT = Pattern.compile("#,#*(#|0+)").asPredicate();

    public abstract long parseLong(String s) throws ParseException;
    public abstract double parseDouble(String s) throws ParseException;
    public abstract BigDecimal parseBigDecimal(String s) throws ParseException;
    public abstract String toString(long l);
    public abstract String toString(double d);
    public abstract String toString(BigDecimal bd);

    public static IntegerParser of()
    {
        return new BasicIntegerParser();
    }

    public static IntegerParser of(String format)
    {
        if (!isValidFormat(format))
        {
            throw new IllegalArgumentException("Invalid format string: " + format);
        }
        return new FormatIntegerParser(format);
    }

    public static boolean isValidFormat(String format)
    {
        return VALID_FORMAT.test(format);
    }

    private static class BasicIntegerParser extends IntegerParser
    {
        private static final String INVALID_INTEGER_MESSAGE = "Should be digits optionally preceded by '+' or '-'";

        @Override
        public long parseLong(String s) throws ParseException
        {
            try
            {
                return Long.parseLong(s);
            }
            catch (NumberFormatException e)
            {
                throw new ParseException(INVALID_INTEGER_MESSAGE, 0);
            }
        }

        @Override
        public double parseDouble(String s) throws ParseException
        {
            try
            {
                return Double.parseDouble(s);
            }
            catch (NumberFormatException e)
            {
                throw new ParseException(INVALID_INTEGER_MESSAGE, 0);
            }
        }

        @Override
        public BigDecimal parseBigDecimal(String s) throws ParseException
        {
            try
            {
                return new BigDecimal(s);
            }
            catch (NumberFormatException e)
            {
                throw new ParseException(INVALID_INTEGER_MESSAGE, 0);
            }
        }

        @Override
        public String validate(String s)
        {
            return DEFAULT_IS_VALID_INTEGER.test(s) ? null : INVALID_INTEGER_MESSAGE;
        }

        @Override
        public String toString(long l)
        {
            return String.valueOf(l);
        }

        @Override
        public String toString(double d)
        {
            return String.valueOf((long) d);
        }

        @Override
        public String toString(BigDecimal bd)
        {
            return bd.toBigInteger().toString();
        }
    }

    private static class FormatIntegerParser extends IntegerParser
    {
        private final ThreadLocal<DecimalFormat> format;
        private final String formatString;
        private final Predicate<String> isValidInteger;

        FormatIntegerParser(String formatString)
        {
            this.format = ThreadLocal.withInitial(() -> new DecimalFormat(formatString));
            this.formatString = formatString;
            DecimalFormat fmt = format.get();
            isValidInteger = fmt.isGroupingUsed()
                    ? Pattern.compile("[+-]?\\d{1,"+fmt.getGroupingSize()+"}(\\d{"+fmt.getGroupingSize()+"},)*").asPredicate()
                    : DEFAULT_IS_VALID_INTEGER;
        }

        @Override
        public long parseLong(String s) throws ParseException
        {
            try
            {
                DecimalFormat fmt = format.get();
                fmt.setParseBigDecimal(false);
                Number parsed = fmt.parse(s, new ParsePosition(0));
                if (!(parsed instanceof Long))
                {
                    throw new ParseException("Number out of range", 0);
                }
                return parsed.longValue();
            }
            catch (Exception e)
            {
                throw new ParseException(invalidIntegerMessage(), 0);
            }
        }

        @Override
        public double parseDouble(String s) throws ParseException
        {
            try
            {
                DecimalFormat fmt = format.get();
                fmt.setParseBigDecimal(false);
                return format.get().parse(s).doubleValue();
            }
            catch (Exception e)
            {
                throw new ParseException(invalidIntegerMessage(), 0);
            }
        }

        @Override
        public BigDecimal parseBigDecimal(String s) throws ParseException
        {
            try
            {
                DecimalFormat fmt = format.get();
                fmt.setParseBigDecimal(true);
                return (BigDecimal) format.get().parse(s);
            }
            catch (Exception e)
            {
                throw new ParseException(invalidIntegerMessage(), 0);
            }
        }

        @Override
        public String toString(long l)
        {
            return format.get().format(l);
        }

        @Override
        public String toString(double d)
        {
            return format.get().format((long) d);
        }

        @Override
        public String toString(BigDecimal bd)
        {
            return format.get().format(bd.toBigInteger());
        }

        @Override
        public String validate(String s)
        {
            return isValidInteger.test(s) ? null : invalidIntegerMessage();
        }

        private String invalidIntegerMessage()
        {
            return "Should conform to the format: " + formatString;
        }
    }
}
