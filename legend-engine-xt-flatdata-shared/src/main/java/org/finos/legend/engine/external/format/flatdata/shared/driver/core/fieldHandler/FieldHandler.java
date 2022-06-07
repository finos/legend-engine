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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.ValueParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Function;

public abstract class FieldHandler
{
    private final FlatDataRecordField field;
    private final int fieldIndex;
    private final ValueParser parser;
    private final Function<RawFlatData, String> rawDataAccessor;

    FieldHandler(FlatDataRecordField field, int fieldIndex, ValueParser parser, Function<RawFlatData, String> rawDataAccessor)
    {
        this.field = field;
        this.fieldIndex = fieldIndex;
        this.parser = parser;
        this.rawDataAccessor = rawDataAccessor;
    }

    public FlatDataRecordField getField()
    {
        return field;
    }

    public int getFieldIndex()
    {
        return fieldIndex;
    }

    public String rawValue(RawFlatData rawData)
    {
        return rawDataAccessor.apply(rawData);
    }

    public boolean hasRawValue(RawFlatData rawData)
    {
        return rawValue(rawData) != null;
    }

    public String validate(RawFlatData rawData)
    {
        return validate(rawDataAccessor.apply(rawData));
    }

    public String validate(String raw)
    {
        return parser.validate(raw);
    }

    public String getString(RawFlatData rawData) throws ParseException
    {
        return getString(rawDataAccessor.apply(rawData));
    }

    public boolean getBoolean(RawFlatData rawData) throws ParseException
    {
        return getBoolean(rawDataAccessor.apply(rawData));
    }

    public long getLong(RawFlatData rawData) throws ParseException
    {
        return getLong(rawDataAccessor.apply(rawData));
    }

    public double getDouble(RawFlatData rawData) throws ParseException
    {
        return getDouble(rawDataAccessor.apply(rawData));
    }

    public BigDecimal getBigDecimal(RawFlatData rawData) throws ParseException
    {
        return getBigDecimal(rawDataAccessor.apply(rawData));
    }

    public LocalDate getLocalDate(RawFlatData rawData) throws ParseException
    {
        return getLocalDate(rawDataAccessor.apply(rawData));
    }

    public Instant getInstant(RawFlatData rawData) throws ParseException
    {
        return getInstant(rawDataAccessor.apply(rawData));
    }

    String getString(String raw) throws ParseException
    {
        throw new ParseException("Not a string value", 0);
    }

    boolean getBoolean(String raw) throws ParseException
    {
        throw new ParseException("Not a boolean value", 0);
    }

    long getLong(String raw) throws ParseException
    {
        throw new ParseException("Not a suitable numeric value", 0);
    }

    double getDouble(String raw) throws ParseException
    {
        throw new ParseException("Not a numeric value", 0);
    }

    BigDecimal getBigDecimal(String raw) throws ParseException
    {
        throw new ParseException("Not a numeric value", 0);
    }

    LocalDate getLocalDate(String raw) throws ParseException
    {
        throw new ParseException("Not a date value", 0);
    }

    Instant getInstant(String raw) throws ParseException
    {
        throw new ParseException("Not a datetime value", 0);
    }
}
