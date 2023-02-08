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

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.zone.ZoneRules;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.Nonnull;


final class DateType extends BaseTimestampType
{

  public static final PGType INSTANCE = new DateType();

  private static final int OID = 1082;
  private static final String NAME = "date";

  private static final DateTimeFormatter ISO_FORMATTER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .append(ISO_LOCAL_DATE)
      .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);

  private static final DateTimeFormatter ISO_FORMATTER_AD = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("yyyy-MM-dd")
      .toFormatter(Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT);

  private DateType()
  {
    super(OID, TYPE_LEN, TYPE_MOD, NAME);
  }

  @Override
  public int typArray()
  {
    return PGArray.DATE_ARRAY.oid();
  }

  @Override
  byte[] encodeAsUTF8Text(@Nonnull Object value)
  {
    long millis = (long) value;
    LocalDate date = ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);

    return date.format(ISO_FORMATTER_AD).getBytes(StandardCharsets.UTF_8);
  }

  public static LocalDate ofInstant(Instant instant, ZoneId zone)
  {
    Objects.requireNonNull(instant, "instant");
    Objects.requireNonNull(zone, "zone");
    ZoneRules rules = zone.getRules();
    ZoneOffset offset = rules.getOffset(instant);
    long localSecond = instant.getEpochSecond() + offset.getTotalSeconds();
    long localEpochDay = Math.floorDiv(localSecond, /*LocalTime.SECONDS_PER_DAY*/ (long) 86400);
    return LocalDate.ofEpochDay(localEpochDay);
  }

  @Override
  Object decodeUTF8Text(byte[] bytes)
  {
    String s = new String(bytes, StandardCharsets.UTF_8);

    //TODO: Add support of other formats, other than ISO 8601 (YYYY-MM-DD).
    LocalDate dt = LocalDate.parse(s, ISO_FORMATTER);
    return dt.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
  }
}
