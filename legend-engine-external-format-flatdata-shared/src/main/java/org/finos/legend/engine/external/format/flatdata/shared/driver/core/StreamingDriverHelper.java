package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.BooleanParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DateParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DateTimeParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DecimalParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.IntegerParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.StringParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.ValueParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.IntegerVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.StringListVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.StringVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.VariableType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;

import java.util.Collections;
import java.util.List;

public class StreamingDriverHelper
{
    public static final String RECORD_SEPARATOR = "recordSeparator";
    public static final String MAY_CONTAIN_BLANK_LINES = "mayContainBlankLines";
    public static final String SCOPE = "scope";
    public static final String UNTIL_EOF = "untilEof";
    public static final String DEFAULT = "default";
    public static final String FOR_NUMBER_OF_LINES = "forNumberOfLines";
    public static final String UNTIL_LINE_EQUALS = "untilLineEquals";

    final StringVariable defaultTrueString;
    final StringVariable defaultFalseString;
    final StringVariable defaultIntegerFormat;
    final StringVariable defaultDecimalFormat;
    final StringListVariable defaultDateFormat;
    final StringListVariable defaultDateTimeFormat;
    final StringVariable defaultTimeZone;

    public static final FlatDataVariable VARIABLE_LINE_NUMBER = new FlatDataVariable("lineNumber", VariableType.Integer);

    public final FlatDataProcessingContext context;
    public final FlatDataSection section;
    public final boolean skipBlankLines;
    public final String eol;

    public StreamingDriverHelper(FlatDataSection section, FlatDataProcessingContext context)
    {
        this.section = section;
        List<FlatDataProperty> properties = section.getSectionProperties();
        this.eol = FlatDataUtils.getString(properties, RECORD_SEPARATOR).orElse(null);
        this.skipBlankLines = FlatDataUtils.getBoolean(properties, MAY_CONTAIN_BLANK_LINES);
        this.context = context;

        this.defaultTrueString = FlatDataUtils.getString(properties, FlatDataUtils.DEFAULT_TRUE_STRING)
                                              .map(str -> StringVariable.initializeIfMissing(context, FlatDataUtils.VARIABLE_DEFAULT_TRUE_STRING, str))
                                              .orElse(StringVariable.reference(context, FlatDataUtils.VARIABLE_DEFAULT_TRUE_STRING));

        this.defaultFalseString = FlatDataUtils.getString(properties, FlatDataUtils.DEFAULT_FALSE_STRING)
                                               .map(str -> StringVariable.initializeIfMissing(context, FlatDataUtils.VARIABLE_DEFAULT_FALSE_STRING, str))
                                               .orElse(StringVariable.reference(context, FlatDataUtils.VARIABLE_DEFAULT_FALSE_STRING));

        defaultIntegerFormat = FlatDataUtils.getString(properties, FlatDataUtils.DEFAULT_INTEGER_FORMAT)
                                            .map(fmt -> StringVariable.initializeIfMissing(context, FlatDataUtils.VARIABLE_DEFAULT_INTEGER_FORMAT, fmt))
                                            .orElse(StringVariable.reference(context, FlatDataUtils.VARIABLE_DEFAULT_INTEGER_FORMAT));

        defaultDecimalFormat = FlatDataUtils.getString(properties, FlatDataUtils.DEFAULT_DECIMAL_FORMAT)
                                            .map(fmt -> StringVariable.initializeIfMissing(context, FlatDataUtils.VARIABLE_DEFAULT_DECIMAL_FORMAT, fmt))
                                            .orElse(StringVariable.reference(context, FlatDataUtils.VARIABLE_DEFAULT_DECIMAL_FORMAT));

        defaultDateFormat = StringListVariable.initializeIfMissing(context, FlatDataUtils.VARIABLE_DEFAULT_DATE_FORMAT, FlatDataUtils.getStrings(properties, FlatDataUtils.DEFAULT_DATE_FORMAT).orElse(Collections.singletonList(FlatDataUtils.ISO_DATE_FORMAT)));
        defaultDateTimeFormat = StringListVariable.initializeIfMissing(context, FlatDataUtils.VARIABLE_DEFAULT_DATETIME_FORMAT, FlatDataUtils.getStrings(properties, FlatDataUtils.DEFAULT_DATETIME_FORMAT).orElse(Collections.singletonList(FlatDataUtils.ISO_DATETIME_FORMAT)));
        defaultTimeZone = StringVariable.initializeIfMissing(context, FlatDataUtils.VARIABLE_DEFAULT_TIME_ZONE, FlatDataUtils.getString(properties, FlatDataUtils.DEFAULT_TIME_ZONE).orElse(FlatDataUtils.TIME_ZONE));
    }

    public IntegerVariable lineNumber()
    {
        return IntegerVariable.initializeIfMissing(context, VARIABLE_LINE_NUMBER, 0);
    }

    public List<ValueParser> computeValueParsers(FlatDataRecordType recordType)
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
                String trueString = booleanType.getTrueString() == null && defaultTrueString.isSet() ? defaultTrueString.get() : booleanType.getTrueString();
                String falseString = booleanType.getFalseString() == null && defaultFalseString.isSet() ? defaultFalseString.get() : booleanType.getFalseString();
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
}
