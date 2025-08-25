/*
 * Licensed to Crate.io GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package org.finos.legend.engine.postgres.types;

/*import io.crate.metadata.CoordinatorTxnCtx;
import io.crate.types.DataTypes;*/

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.time.format.DateTimeFormatter;

final class TimestampZType extends BaseTimestampType
{

    public static final PGType INSTANCE = new TimestampZType();

    private static final int OID = 1184;
    private static final String NAME = "timestamptz";

    // For Golang if date is AD (after Christ), era abbreviation is not parsed.
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS+00", Locale.ENGLISH).withZone(ZoneId.of("UTC"));

    // For Golang if date is BC (before Christ), era abbreviation needs to be appended.
    private static final DateTimeFormatter ISO_FORMATTER_WITH_ERA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS+00 G", Locale.ENGLISH).withZone(ZoneId.of("UTC"));

    private static final DateTimeFormatter[] PARSERS_WITHOUT_ERA = generateParseFormatters(false);
    private static final DateTimeFormatter[] PARSERS_WITH_ERA = generateParseFormatters(true);

    private static DateTimeFormatter[] generateParseFormatters(boolean withEra)
    {
        DateTimeFormatter[] formatters = new DateTimeFormatter[10];
        String prefix = "yyyy-MM-dd HH:mm:ss";

        String suffix = "[XXX][XX][X]";
        if (withEra)
        {
            suffix += " G";
        }

        formatters[0] = DateTimeFormatter.ofPattern(prefix + suffix).withLocale(Locale.ENGLISH);
        for (int i = 1; i < 10; i++)
        { // 1-9 digits for fraction of second
            StringBuilder pattern = new StringBuilder(prefix);
            pattern.append('.');
            for (int j = 1; j <= i; j++)
            {
                pattern.append('S');
            }
            pattern.append(suffix);
            formatters[i] = DateTimeFormatter.ofPattern(pattern.toString()).withLocale(Locale.ENGLISH);
        }
        return formatters;
    }

    private TimestampZType()
    {
        super(OID, TYPE_LEN, TYPE_MOD, NAME);
    }

    @Override
    public int typArray()
    {
        return PGArray.TIMESTAMPZ_ARRAY.oid();
    }

    @Override
    byte[] encodeAsUTF8Text(Object value)
    {
        long msecs = (long) value;
        if (msecs >= FIRST_MSEC_AFTER_CHRIST)
        {
            return ISO_FORMATTER.format(Instant.ofEpochMilli(msecs)).getBytes(StandardCharsets.UTF_8);
        }
        else
        {
            return ISO_FORMATTER_WITH_ERA.format(Instant.ofEpochMilli(msecs)).getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    Object decodeUTF8Text(byte[] bytes)
    {
        // Currently seems that only GoLang prepared statements are sent as TimestampType with time zone
        // Other PostgreSQL clients send the parameter as Bigint or Varchar
        String s = new String(bytes, StandardCharsets.UTF_8);
       /* try {
            return DataTypes.TIMESTAMPZ.explicitCast(s, CoordinatorTxnCtx.systemTransactionContext().sessionSettings());
        } catch (Exception e) {*/
        int endOfSeconds = s.indexOf(".");
        int idx = endOfSeconds;
        if (endOfSeconds > 0)
        {
            idx++;
            while (s.charAt(idx) != '+' && s.charAt(idx) != '-')
            { // start of timezone
                idx++;
            }
        }

        int fractionDigits = idx - endOfSeconds - 1;
        fractionDigits = fractionDigits < 0 ? 0 : fractionDigits;
        if (fractionDigits > 9)
        {
            throw new IllegalArgumentException(
                    "Cannot parse more than 9 digits for fraction of a second");
        }

        boolean withEra = s.endsWith("BC") || s.endsWith("AD");
        if (withEra)
        {
            return Instant.from(PARSERS_WITH_ERA[fractionDigits].parse(s)).toEpochMilli();
        }
        return Instant.from(PARSERS_WITHOUT_ERA[fractionDigits].parse(s)).toEpochMilli();
    }
}
