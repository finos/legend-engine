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

package org.finos.legend.engine.plan.dependencies.domain.date;

import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFormatPattern;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Reads a date format string the way this platform has always read it, and leaves the rendering to Pure's
 * implementation of the format language.
 *
 * <p>This platform used to carry an implementation of that language of its own, and it gave some strings a
 * different meaning from Pure's. A run of {@code S} is a fixed width here, padding a short fraction and writing
 * zeros for a date with no fraction at all, where to Pure it is at most that many digits. And a component the
 * date does not carry writes a default here rather than failing: an hour, minute, or second writes zeros,
 * {@code h} writes 12, {@code a} writes AM, and an offset writes {@code +0000} or {@code Z}. Strings in stored
 * models depend on both.
 *
 * <p>So a string written entirely in the syntax this platform has always accepted is first rewritten into the
 * spelling that tells Pure what it has always meant here: {@code SSS} becomes {@code ?[S3|"000"]}, and
 * {@code HH} becomes {@code ?[HH|"00"]}. A string using anything Pure's language has added since, a sub-second
 * width or an optional section, cannot have been written for this platform, which rejected those characters,
 * so it is parsed as it stands and means exactly what it means to Pure.
 *
 * <p>Two things are not carried over, because what this platform did with them was wrong rather than merely
 * different. A time zone is read as Pure reads one: only at the start of the string, never silently replaced
 * by GMT when its name is not recognized, and written by {@code z} as the string names it. And a two digit
 * year is written as Pure writes it, without a sign.
 *
 * <p>Parsing a string costs more than rendering a date with it, and a serializer renders every date in a result
 * with the same string, so patterns are kept in a small table keyed by their string. A collision overwrites
 * whatever was there, so the table never grows however many strings pass through it, and a pattern holds
 * nothing between renderings, so one may be shared by every thread.
 */
final class LegacyDateFormat
{
    private static final int CACHE_SIZE = 64;

    private static final AtomicReferenceArray<CachedPattern> CACHE = new AtomicReferenceArray<>(CACHE_SIZE);

    private LegacyDateFormat()
    {
    }

    /**
     * The pattern a format string stands for on this platform.
     *
     * @param formatString format string
     * @return the pattern it stands for
     * @throws IllegalArgumentException if it is not a valid format string
     */
    static DateFormatPattern pattern(String formatString)
    {
        int hash = formatString.hashCode();
        int slot = (hash ^ (hash >>> 16)) & (CACHE_SIZE - 1);
        CachedPattern cached = CACHE.get(slot);
        if ((cached != null) && cached.formatString.equals(formatString))
        {
            return cached.pattern;
        }

        String rewritten = rewrite(formatString);
        DateFormatPattern pattern = DateFormatPattern.parse((rewritten == null) ? formatString : rewritten);
        CACHE.set(slot, new CachedPattern(formatString, pattern));
        return pattern;
    }

    /**
     * Rewrite a format string written in the syntax this platform has always accepted into the spelling that
     * tells Pure what it has always meant here.
     *
     * @param formatString format string
     * @return the rewritten string, or null if the string is not written entirely in that syntax
     */
    static String rewrite(String formatString)
    {
        int length = formatString.length();
        StringBuilder builder = new StringBuilder(length * 2);
        int index = 0;
        while (index < length)
        {
            char character = formatString.charAt(index);
            switch (character)
            {
                case '[':
                {
                    int end = findEndOfTimeZone(formatString, index + 1);
                    if (end == -1)
                    {
                        return null;
                    }
                    builder.append(formatString, index, end);
                    index = end;
                    break;
                }
                case '"':
                {
                    int end = findEndOfQuotedText(formatString, index + 1);
                    if (end == -1)
                    {
                        return null;
                    }
                    builder.append(formatString, index, end);
                    index = end;
                    break;
                }
                case 'y':
                case 'M':
                case 'd':
                case 'z':
                {
                    // these mean the same thing on both platforms, or are written as Pure writes them
                    int end = findEndOfRun(formatString, index);
                    builder.append(formatString, index, end);
                    index = end;
                    break;
                }
                case 'h':
                {
                    int end = findEndOfRun(formatString, index);
                    appendDefaulted(builder, formatString, index, end, zeroPadded("12", end - index));
                    index = end;
                    break;
                }
                case 'H':
                case 'm':
                case 's':
                {
                    int end = findEndOfRun(formatString, index);
                    appendDefaulted(builder, formatString, index, end, zeroPadded("", end - index));
                    index = end;
                    break;
                }
                case 'S':
                {
                    int end = findEndOfRun(formatString, index);
                    int width = end - index;
                    builder.append("?[S").append(width).append("|\"").append(zeroPadded("", width)).append("\"]");
                    index = end;
                    break;
                }
                case 'a':
                {
                    // this platform writes AM or PM once for every letter rather than once for the run
                    builder.append("?[a|\"AM\"]");
                    index++;
                    break;
                }
                case 'Z':
                {
                    int end = findEndOfRun(formatString, index);
                    appendDefaulted(builder, formatString, index, end, "+0000");
                    index = end;
                    break;
                }
                case 'X':
                {
                    int end = findEndOfRun(formatString, index);
                    appendDefaulted(builder, formatString, index, end, "Z");
                    index = end;
                    break;
                }
                case '-':
                case '/':
                case ':':
                case '.':
                case ' ':
                case '\t':
                {
                    builder.append(character);
                    index++;
                    break;
                }
                default:
                {
                    return null;
                }
            }
        }
        return builder.toString();
    }

    private static void appendDefaulted(StringBuilder builder, String formatString, int start, int end, String fallback)
    {
        builder.append("?[").append(formatString, start, end).append("|\"").append(fallback).append("\"]");
    }

    private static String zeroPadded(String text, int width)
    {
        StringBuilder builder = new StringBuilder(Math.max(width, text.length()));
        for (int i = text.length(); i < width; i++)
        {
            builder.append('0');
        }
        return builder.append(text).toString();
    }

    private static int findEndOfRun(String formatString, int start)
    {
        char character = formatString.charAt(start);
        int end = start + 1;
        while ((end < formatString.length()) && (formatString.charAt(end) == character))
        {
            end++;
        }
        return end;
    }

    private static int findEndOfTimeZone(String formatString, int start)
    {
        boolean escaped = false;
        boolean inQuotes = false;
        for (int i = start; i < formatString.length(); i++)
        {
            char next = formatString.charAt(i);
            if (escaped)
            {
                escaped = false;
            }
            else if (next == '"')
            {
                inQuotes = !inQuotes;
            }
            else if ((next == ']') && !inQuotes)
            {
                return i + 1;
            }
            else if (next == '\\')
            {
                escaped = true;
            }
        }
        return -1;
    }

    private static int findEndOfQuotedText(String formatString, int start)
    {
        boolean escaped = false;
        for (int i = start; i < formatString.length(); i++)
        {
            char next = formatString.charAt(i);
            if (escaped)
            {
                escaped = false;
            }
            else if (next == '"')
            {
                return i + 1;
            }
            else if (next == '\\')
            {
                escaped = true;
            }
        }
        return -1;
    }

    private static final class CachedPattern
    {
        private final String formatString;
        private final DateFormatPattern pattern;

        private CachedPattern(String formatString, DateFormatPattern pattern)
        {
            this.formatString = formatString;
            this.pattern = pattern;
        }
    }
}
