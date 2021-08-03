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
import java.util.function.Predicate;
import java.util.regex.Pattern;

public abstract class DecimalParser implements ValueParser
{
    private static final Predicate<String> DEFAULT_IS_VALID_DECIMAL = Pattern.compile("[+-]?(\\d+|\\d*\\.\\d+|\\d+\\.\\d*)([eE]\\d+)?").asPredicate();
    private static final Predicate<String> VALID_FORMAT = Pattern.compile("#,#*(#|0+)(\\.0*#*)?(E\\d+)?").asPredicate();

    public abstract double parseDouble(String s) throws ParseException;
    public abstract BigDecimal parseBigDecimal(String s) throws ParseException;
    public abstract String toString(double d);
    public abstract String toString(BigDecimal bd);

    public static DecimalParser of()
    {
        return new BasicDecimalParser();
    }

    public static DecimalParser of(String format)
    {
        if (!isValidFormat(format))
        {
            throw new IllegalArgumentException("Invalid format string: " + format);
        }
        return new FormatDecimalParser(format);
    }

    public static boolean isValidFormat(String format)
    {
        return VALID_FORMAT.test(format);
    }

    private static class BasicDecimalParser extends DecimalParser
    {
        private static final String INVALID_DECIMAL_MESSAGE = "Should be an optionally signed simple floating point number or one in scientific notation";

        @Override
        public double parseDouble(String s) throws ParseException
        {
            try
            {
                return Double.parseDouble(s);
            }
            catch (NumberFormatException e)
            {
                throw new ParseException(INVALID_DECIMAL_MESSAGE, 0);
            }
        }

        @Override
        public BigDecimal parseBigDecimal(String s) throws ParseException
        {
            try
            {
                return new BigDecimal(s).stripTrailingZeros();
            }
            catch (NumberFormatException e)
            {
                throw new ParseException(INVALID_DECIMAL_MESSAGE, 0);
            }
        }

        @Override
        public String validate(String s)
        {
            return DEFAULT_IS_VALID_DECIMAL.test(s) ? null : INVALID_DECIMAL_MESSAGE;
        }

        @Override
        public String toString(double d)
        {
            return String.valueOf(d);
        }

        @Override
        public String toString(BigDecimal bd)
        {
            return bd.toPlainString();
        }
    }

    private static class FormatDecimalParser extends DecimalParser
    {
        private final ThreadLocal<DecimalFormat> format;
        private final String formatString;
        private final Predicate<String> isDecimalInteger;

        FormatDecimalParser(String formatString)
        {
            this.format = ThreadLocal.withInitial(() -> new DecimalFormat(formatString));
            this.formatString = formatString;
            DecimalFormat fmt = format.get();
            isDecimalInteger = fmt.isGroupingUsed()
                    ? Pattern.compile("[+-]?\\d{1,"+fmt.getGroupingSize()+"}(\\d{"+fmt.getGroupingSize()+"},)*" + (formatString.contains("E") ? "([eE]\\d+)?" : "")).asPredicate()
                    : DEFAULT_IS_VALID_DECIMAL;
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
        public String validate(String s)
        {
            return isDecimalInteger.test(s) ? null : invalidIntegerMessage();
        }

        @Override
        public String toString(double d)
        {
            return format.get().format(d);
        }

        @Override
        public String toString(BigDecimal bd)
        {
            return format.get().format(bd);
        }

        private String invalidIntegerMessage()
        {
            return "Should conform to the format: " + formatString;
        }
    }
}
