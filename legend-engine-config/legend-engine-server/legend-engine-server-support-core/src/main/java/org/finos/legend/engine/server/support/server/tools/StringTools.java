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

package org.finos.legend.engine.server.support.server.tools;

import java.net.UnknownHostException;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public class StringTools
{
    private StringTools()
    {
    }

    // Exception message utilities

    private static final String DEFAULT_EX_MESSAGE_SEPARATOR = ": ";

    public static String appendThrowableMessageIfPresent(String prefix, Throwable t)
    {
        return appendThrowableMessageIfPresent(prefix, t, (Function<Throwable, String>)null);
    }

    public static <T extends Throwable> String appendThrowableMessageIfPresent(String prefix, T t, Function<? super T, String> messageFunc)
    {
        return appendThrowableMessageIfPresent(prefix, t, messageFunc, ((prefix == null) || prefix.isEmpty()) ? null : DEFAULT_EX_MESSAGE_SEPARATOR);
    }

    public static String appendThrowableMessageIfPresent(String prefix, Throwable t, String separator)
    {
        return appendThrowableMessageIfPresent(prefix, t, null, separator);
    }

    public static <T extends Throwable> String appendThrowableMessageIfPresent(String prefix, T t, Function<? super T, String> messageFunc, String separator)
    {
        if (t == null)
        {
            return prefix;
        }
        if ((prefix == null) || prefix.isEmpty())
        {
            StringBuilder builder = appendThrowableMessageIfPresent(new StringBuilder(), t, messageFunc, separator);
            return (builder.length() == 0) ? prefix : builder.toString();
        }
        return appendThrowableMessageIfPresent(new StringBuilder(prefix), t, messageFunc, separator).toString();
    }

    public static StringBuilder appendThrowableMessageIfPresent(StringBuilder builder, Throwable t)
    {
        return appendThrowableMessageIfPresent(builder, t, (Function<Throwable, String>)null);
    }

    public static <T extends Throwable> StringBuilder appendThrowableMessageIfPresent(StringBuilder builder, T t, Function<? super T, String> messageFunc)
    {
        return appendThrowableMessageIfPresent(builder, t, messageFunc, (builder.length() == 0) ? null : DEFAULT_EX_MESSAGE_SEPARATOR);
    }

    public static StringBuilder appendThrowableMessageIfPresent(StringBuilder builder, Throwable t, String separator)
    {
        return appendThrowableMessageIfPresent(builder, t, null, separator);
    }

    public static <T extends Throwable> StringBuilder appendThrowableMessageIfPresent(StringBuilder builder, T t, Function<? super T, String> messageFunc, String separator)
    {
        if (t != null)
        {
            String message = (messageFunc == null) ? t.getMessage() : messageFunc.apply(t);
            if ((message != null) && !message.isEmpty())
            {
                if (separator != null)
                {
                    builder.append(separator);
                }
                if ((t instanceof UnknownHostException) && ((messageFunc == null) || Objects.equals(message, t.getMessage())))
                {
                    builder.append("unknown host - ");
                }
                builder.append(message);
            }
        }
        return builder;
    }

    // Duration formatting

    private static final String ZERO_SECONDS_WITH_NANOS_STRING = "0.000000000";

    public static String formatDurationInNanos(long durationInNanos)
    {
        return (durationInNanos == 0) ? ZERO_SECONDS_WITH_NANOS_STRING : formatDurationInNanos(new StringBuilder(20), durationInNanos).toString();
    }

    public static StringBuilder formatDurationInNanos(StringBuilder builder, long durationInNanos)
    {
        if (durationInNanos == 0)
        {
            return builder.append(ZERO_SECONDS_WITH_NANOS_STRING);
        }

        boolean negative = durationInNanos < 0;
        String string = Long.toString(durationInNanos);
        int secondsCharCount = string.length() - (negative ? 10 : 9);
        if (secondsCharCount <= 0)
        {
            if (negative)
            {
                builder.ensureCapacity(builder.length() + 12);
                zeroFill(builder.append("-0."), 10 - string.length()).append(string, 1, string.length());
            }
            else
            {
                builder.ensureCapacity(builder.length() + 11);
                zeroFill(builder.append("0."), 9 - string.length()).append(string);
            }
            return builder;
        }

        builder.ensureCapacity(builder.length() + string.length() + 1 + ((secondsCharCount - 1) / 3));

        // seconds
        int firstSeparatorIndex = ((secondsCharCount - 1) % 3) + 1 + (negative ? 1 : 0);
        builder.append(string, 0, firstSeparatorIndex);
        for (int i = firstSeparatorIndex; i < secondsCharCount; i += 3)
        {
            builder.append(',').append(string, i, i + 3);
        }

        // subseconds
        return builder.append('.').append(string, string.length() - 9, string.length());
    }

    private static StringBuilder zeroFill(StringBuilder builder, int n)
    {
        builder.ensureCapacity(builder.length() + n);
        for (int i = 0; i < n; i++)
        {
            builder.append('0');
        }
        return builder;
    }

    // Log sanitizing

    private static final Pattern SINGLE_UNSAFE_LOG_MESSAGE_PATTERN = Pattern.compile("[^ \\w\\p{Punct}]");
    private static final Pattern MULTI_UNSAFE_LOG_MESSAGE_PATTERN = Pattern.compile(SINGLE_UNSAFE_LOG_MESSAGE_PATTERN.pattern() + "++");

    public static String sanitizeForLogging(String string, String replacement, boolean replaceGroups)
    {
        Pattern pattern = replaceGroups ? MULTI_UNSAFE_LOG_MESSAGE_PATTERN : SINGLE_UNSAFE_LOG_MESSAGE_PATTERN;
        return pattern.matcher(string).replaceAll(replacement);
    }

    // Vertical whitespace replacement

    private static final Pattern SINGLE_VERTICAL_WHITESPACE_PATTERN = Pattern.compile("\\v");
    private static final Pattern MULTI_VERTICAL_WHITESPACE_PATTERN = Pattern.compile(SINGLE_VERTICAL_WHITESPACE_PATTERN.pattern() + "++");

    public static String replaceVerticalWhitespace(String string, String replacement, boolean replaceGroups)
    {
        Pattern pattern = replaceGroups ? MULTI_VERTICAL_WHITESPACE_PATTERN : SINGLE_VERTICAL_WHITESPACE_PATTERN;
        return pattern.matcher(string).replaceAll(replacement);
    }
}
