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

package org.finos.legend.engine.external.format.flatdata.driver.core.util;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.BooleanFieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.DateFieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.DateTimeFieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.DecimalFieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.IntegerFieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.StringFieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.valueParser.BooleanParser;
import org.finos.legend.engine.external.format.flatdata.driver.core.valueParser.DateParser;
import org.finos.legend.engine.external.format.flatdata.driver.core.valueParser.DateTimeParser;
import org.finos.legend.engine.external.format.flatdata.driver.core.valueParser.DecimalParser;
import org.finos.legend.engine.external.format.flatdata.driver.core.valueParser.IntegerParser;
import org.finos.legend.engine.external.format.flatdata.driver.core.valueParser.StringParser;
import org.finos.legend.engine.external.format.flatdata.driver.core.valueParser.ValueParser;
import org.finos.legend.engine.external.format.flatdata.driver.core.variables.StringListVariable;
import org.finos.legend.engine.external.format.flatdata.driver.core.variables.StringVariable;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.spi.VariableType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataString;

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
        this(Objects.requireNonNull(section.recordType), section.sectionProperties, context);
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

        List<ValueParser> result = Lists.mutable.withInitialCapacity(recordType.fields.size());
        for (int i = 0; i < recordType.fields.size(); i++)
        {
            FlatDataRecordField field = recordType.fields.get(i);
            FlatDataDataType type = field.type;
            if (type instanceof FlatDataString)
            {
                result.add(StringParser.of());
            }
            else if (type instanceof FlatDataBoolean)
            {
                FlatDataBoolean booleanType = (FlatDataBoolean) type;
                String trueString;
                String falseString;
                if (booleanType.trueString != null || booleanType.falseString != null)
                {
                    trueString = booleanType.trueString;
                    falseString = booleanType.falseString;
                }
                else
                {
                    trueString = defaultTrueString.isSet() ? defaultTrueString.get() : booleanType.trueString;
                    falseString = defaultFalseString.isSet() ? defaultFalseString.get() : booleanType.falseString;
                }
                result.add(BooleanParser.of(trueString, falseString));
            }
            else if (type instanceof FlatDataInteger)
            {
                FlatDataInteger integerType = (FlatDataInteger) type;
                String format = integerType.format == null ? (defaultIntegerFormat.isSet() ? defaultIntegerFormat.get() : null) : integerType.format;
                result.add(format == null ? IntegerParser.of() : IntegerParser.of(format));
            }
            else if (type instanceof FlatDataDecimal)
            {
                FlatDataDecimal decimalType = (FlatDataDecimal) type;
                String format = decimalType.format == null ? (defaultDecimalFormat.isSet() ? defaultDecimalFormat.get() : null) : decimalType.format;
                result.add(format == null ? DecimalParser.of() : DecimalParser.of(format));
            }
            else if (type instanceof FlatDataDate)
            {
                FlatDataDate dateType = (FlatDataDate) type;
                if (!dateType.format.isEmpty())
                {
                    result.add(DateParser.of(dateType.format));
                }
                else
                {
                    result.add(defaultDateParser);
                }
            }
            else if (type instanceof FlatDataDateTime)
            {
                FlatDataDateTime dateTimeType = (FlatDataDateTime) type;
                String timeZone = dateTimeType.timeZone == null ? defaultTimeZone.get() : dateTimeType.timeZone;
                if (!dateTimeType.format.isEmpty())
                {
                    result.add(DateTimeParser.of(dateTimeType.format, timeZone));
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
        for (int i = 0; i < recordType.fields.size(); i++)
        {
            FlatDataRecordField field = recordType.fields.get(i);
            FlatDataDataType type = field.type;
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
