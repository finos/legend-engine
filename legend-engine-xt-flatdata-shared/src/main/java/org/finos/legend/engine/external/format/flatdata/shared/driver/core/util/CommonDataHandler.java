// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver.core.util;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.*;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.*;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.StringListVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.StringVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.*;
import org.finos.legend.engine.external.format.flatdata.shared.model.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class CommonDataHandler
{
    public static final String TIME_ZONE = "UTC";
    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXX";

    public static final String DEFAULT_DATE_FORMAT = "defaultDateFormat";
    public static final String DEFAULT_DATETIME_FORMAT = "defaultDateTimeFormat";
    public static final String DEFAULT_TIME_ZONE = "defaultTimeZone";
    public static final String DEFAULT_TRUE_STRING = "defaultTrueString";
    public static final String DEFAULT_FALSE_STRING = "defaultFalseString";
    public static final String DEFAULT_INTEGER_FORMAT = "defaultIntegerFormat";
    public static final String DEFAULT_DECIMAL_FORMAT = "defaultDecimalFormat";

    public static final FlatDataVariable VARIABLE_DEFAULT_DATE_FORMAT = new FlatDataVariable(DEFAULT_DATE_FORMAT, VariableType.StringList);
    public static final FlatDataVariable VARIABLE_DEFAULT_DATETIME_FORMAT = new FlatDataVariable(DEFAULT_DATETIME_FORMAT, VariableType.StringList);
    public static final FlatDataVariable VARIABLE_DEFAULT_TIME_ZONE = new FlatDataVariable(DEFAULT_TIME_ZONE, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_TRUE_STRING = new FlatDataVariable(DEFAULT_TRUE_STRING, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_FALSE_STRING = new FlatDataVariable(DEFAULT_FALSE_STRING, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_INTEGER_FORMAT = new FlatDataVariable(DEFAULT_INTEGER_FORMAT, VariableType.String);
    public static final FlatDataVariable VARIABLE_DEFAULT_DECIMAL_FORMAT = new FlatDataVariable(DEFAULT_DECIMAL_FORMAT, VariableType.String);

    public final FlatDataRecordType recordType;
    private final StringVariable defaultTrueString;
    private final StringVariable defaultFalseString;
    private final StringVariable defaultIntegerFormat;
    private final StringVariable defaultDecimalFormat;
    private final StringListVariable defaultDateFormat;
    private final StringListVariable defaultDateTimeFormat;
    private final StringVariable defaultTimeZone;

    public CommonDataHandler(FlatDataSection section, FlatDataProcessingContext context)
    {
        this(Objects.requireNonNull(section.getRecordType()), section.getSectionProperties(), context);
    }

    public CommonDataHandler(FlatDataRecordType recordType, List<FlatDataProperty> properties, FlatDataProcessingContext context)
    {
        this.recordType = recordType;

        this.defaultTrueString = FlatDataUtils.getString(properties, DEFAULT_TRUE_STRING)
                .map(str -> StringVariable.initializeIfMissing(context, VARIABLE_DEFAULT_TRUE_STRING, str))
                .orElse(StringVariable.reference(context, VARIABLE_DEFAULT_TRUE_STRING));

        this.defaultFalseString = FlatDataUtils.getString(properties, DEFAULT_FALSE_STRING)
                .map(str -> StringVariable.initializeIfMissing(context, VARIABLE_DEFAULT_FALSE_STRING, str))
                .orElse(StringVariable.reference(context, VARIABLE_DEFAULT_FALSE_STRING));

        defaultIntegerFormat = FlatDataUtils.getString(properties, DEFAULT_INTEGER_FORMAT)
                .map(fmt -> StringVariable.initializeIfMissing(context, VARIABLE_DEFAULT_INTEGER_FORMAT, fmt))
                .orElse(StringVariable.reference(context, VARIABLE_DEFAULT_INTEGER_FORMAT));

        defaultDecimalFormat = FlatDataUtils.getString(properties, DEFAULT_DECIMAL_FORMAT)
                .map(fmt -> StringVariable.initializeIfMissing(context, VARIABLE_DEFAULT_DECIMAL_FORMAT, fmt))
                .orElse(StringVariable.reference(context, VARIABLE_DEFAULT_DECIMAL_FORMAT));

        defaultDateFormat = StringListVariable.initializeIfMissing(context, VARIABLE_DEFAULT_DATE_FORMAT, FlatDataUtils.getStrings(properties, DEFAULT_DATE_FORMAT).orElse(Collections.singletonList(ISO_DATE_FORMAT)));
        defaultDateTimeFormat = StringListVariable.initializeIfMissing(context, VARIABLE_DEFAULT_DATETIME_FORMAT, FlatDataUtils.getStrings(properties, DEFAULT_DATETIME_FORMAT).orElse(Collections.singletonList(ISO_DATETIME_FORMAT)));
        defaultTimeZone = StringVariable.initializeIfMissing(context, VARIABLE_DEFAULT_TIME_ZONE, FlatDataUtils.getString(properties, DEFAULT_TIME_ZONE).orElse(TIME_ZONE));
    }

    public List<ValueParser> computeValueParsers()
    {
        DateParser defaultDateParser = DateParser.of(defaultDateFormat.get());
        DateTimeParser defaultDateTimeParser = DateTimeParser.of(defaultDateTimeFormat.get(), defaultTimeZone.get());

        List<ValueParser> result = Lists.mutable.withInitialCapacity(recordType.getFields().size());
        for (int i = 0; i < recordType.getFields().size(); i++)
        {
            FlatDataRecordField field = recordType.getFields().get(i);
            FlatDataDataType type = field.getType();
            if (type instanceof FlatDataString)
            {
                result.add(StringParser.of());
            }
            else if (type instanceof FlatDataBoolean)
            {
                FlatDataBoolean booleanType = (FlatDataBoolean) type;
                String trueString;
                String falseString;
                if (booleanType.getTrueString() != null || booleanType.getFalseString() != null)
                {
                    trueString = booleanType.getTrueString();
                    falseString = booleanType.getFalseString();
                }
                else
                {
                    trueString = defaultTrueString.isSet() ? defaultTrueString.get() : booleanType.getTrueString();
                    falseString = defaultFalseString.isSet() ? defaultFalseString.get() : booleanType.getFalseString();
                }
                result.add(BooleanParser.of(trueString, falseString));
            }
            else if (type instanceof FlatDataInteger)
            {
                FlatDataInteger integerType = (FlatDataInteger) type;
                String format = integerType.getFormat() == null ? (defaultIntegerFormat.isSet() ? defaultIntegerFormat.get() : null) : integerType.getFormat();
                result.add(format == null ? IntegerParser.of() : IntegerParser.of(format));
            }
            else if (type instanceof FlatDataDecimal)
            {
                FlatDataDecimal decimalType = (FlatDataDecimal) type;
                String format = decimalType.getFormat() == null ? (defaultDecimalFormat.isSet() ? defaultDecimalFormat.get() : null) : decimalType.getFormat();
                result.add(format == null ? DecimalParser.of() : DecimalParser.of(format));
            }
            else if (type instanceof FlatDataDate)
            {
                FlatDataDate dateType = (FlatDataDate) type;
                if (!dateType.getFormat().isEmpty())
                {
                    result.add(DateParser.of(dateType.getFormat()));
                }
                else
                {
                    result.add(defaultDateParser);
                }
            }
            else if (type instanceof FlatDataDateTime)
            {
                FlatDataDateTime dateTimeType = (FlatDataDateTime) type;
                String timeZone = dateTimeType.getTimeZone() == null ? defaultTimeZone.get() : dateTimeType.getTimeZone();
                if (!dateTimeType.getFormat().isEmpty())
                {
                    result.add(DateTimeParser.of(dateTimeType.getFormat(), timeZone));
                }
                else
                {
                    result.add(DateTimeParser.of(defaultDateTimeParser, timeZone));
                }
            }
            else
            {
                throw new IllegalArgumentException("Unknown datatype: " + type.getClass().getSimpleName());
            }
        }
        return result;
    }

    public List<FieldHandler> computeFieldHandlers(Function<FlatDataRecordField, Function<RawFlatData, String>> rawDataAccessorFactory)
    {
        List<ValueParser> parsers = computeValueParsers();
        List<FieldHandler> fieldHandlers = Lists.mutable.empty();
        for (int i = 0; i < recordType.getFields().size(); i++)
        {
            FlatDataRecordField field = recordType.getFields().get(i);
            FlatDataDataType type = field.getType();
            if (type instanceof FlatDataString)
            {
                fieldHandlers.add(new StringFieldHandler(field, i, rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataBoolean)
            {
                fieldHandlers.add(new BooleanFieldHandler(field, i, (BooleanParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataInteger)
            {
                fieldHandlers.add(new IntegerFieldHandler(field, i, (IntegerParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataDecimal)
            {
                fieldHandlers.add(new DecimalFieldHandler(field, i, (DecimalParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataDate)
            {
                fieldHandlers.add(new DateFieldHandler(field, i, (DateParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else if (type instanceof FlatDataDateTime)
            {
                fieldHandlers.add(new DateTimeFieldHandler(field, i, (DateTimeParser) parsers.get(i), rawDataAccessorFactory.apply(field)));
            }
            else
            {
                throw new IllegalArgumentException("Unknown datatype: " + type.getClass().getSimpleName());
            }
        }
        return fieldHandlers;
    }

    public static List<PropertyDescription> dataTypeParsingProperties()
    {
        return new PropertyDescription.Builder()
                .optionalStringProperty(DEFAULT_DATE_FORMAT)
                .optionalStringProperty(DEFAULT_DATETIME_FORMAT)
                .optionalStringProperty(DEFAULT_TIME_ZONE)
                .optionalStringProperty(DEFAULT_TRUE_STRING)
                .optionalStringProperty(DEFAULT_FALSE_STRING)
                .optionalStringProperty(DEFAULT_INTEGER_FORMAT)
                .optionalStringProperty(DEFAULT_DECIMAL_FORMAT)
                .build();
    }

    public static List<FlatDataVariable> dataTypeParsingVariables()
    {
        return Arrays.asList(
                VARIABLE_DEFAULT_DATE_FORMAT,
                VARIABLE_DEFAULT_DATETIME_FORMAT,
                VARIABLE_DEFAULT_TIME_ZONE,
                VARIABLE_DEFAULT_TRUE_STRING,
                VARIABLE_DEFAULT_FALSE_STRING,
                VARIABLE_DEFAULT_INTEGER_FORMAT,
                VARIABLE_DEFAULT_DECIMAL_FORMAT
        );
    }
}
