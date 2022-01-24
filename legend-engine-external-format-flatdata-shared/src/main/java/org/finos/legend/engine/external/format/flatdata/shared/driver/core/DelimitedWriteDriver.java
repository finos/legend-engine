package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.ObjectCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.ObjectStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.BooleanParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DateParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DateTimeParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.DecimalParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.IntegerParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.valueParser.ValueParser;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.IntegerVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.Cursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataWriteDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataBoolean;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDataType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDate;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDateTime;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataDecimal;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataInteger;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataString;

import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

public abstract class DelimitedWriteDriver<T> implements FlatDataWriteDriver<T>
{
    public final DelimitedDriverHelper helper;
    public final FlatDataRecordType recordType;
    private final FlatDataProcessingContext context;
    private final ObjectStreamConnection connection;
    private final String nullString;

    DelimitedWriteDriver(DelimitedDriverHelper helper)
    {
        this.context = helper.context;
        this.helper = helper;
        this.recordType = Objects.requireNonNull(helper.section.getRecordType());
        this.connection = (ObjectStreamConnection) context.getConnection();
        this.nullString = helper.nullStrings.isEmpty() ? "" : helper.nullStrings.get(0);
    }

    @Override
    public String getId()
    {
        return DelimitedWithHeadingsDriverDescription.ID;
    }

    @Override
    public boolean canStartAt(Cursor cursor)
    {
        // TODO multi-section writes
        return true;
    }

    public void writeDataLines(PrintWriter writer)
    {
        IntegerVariable lineNumber = helper.lineNumber();
        final List<FlatDataRecordField> fields = recordType.getFields();
        String[] values = new String[fields.size()];

        ObjectToParsedFlatData<? extends T> fromObjectFactory = context.createFromObjectFactory(recordType);
        List<ValueParser> parsers = helper.computeValueParsers(recordType);
        ObjectCursor cursor = connection.getCursor();

        while (!cursor.isEndOfData())
        {
            ParsedFlatData parsed = fromObjectFactory.make(cursor.advance());
            for (int i = 0; i < values.length; i++)
            {
                FlatDataRecordField field = fields.get(i);
                FlatDataDataType type = field.getType();
                if (type instanceof FlatDataString)
                {
                    values[i] = parsed.hasStringValue(field) ? parsed.getString(field) : null;
                }
                else if (type instanceof FlatDataBoolean)
                {
                    values[i] = parsed.hasBooleanValue(field) ? ((BooleanParser) parsers.get(i)).toString(parsed.getBoolean(field)) : null;
                }
                else if (type instanceof FlatDataInteger)
                {
                    if (parsed.hasLongValue(field))
                    {
                        values[i] = ((IntegerParser) parsers.get(i)).toString(parsed.getLong(field));
                    }
                    else if (parsed.hasDoubleValue(field))
                    {
                        values[i] = ((IntegerParser) parsers.get(i)).toString(parsed.getDouble(field));
                    }
                    else if (parsed.hasBigDecimalValue(field))
                    {
                        values[i] = ((IntegerParser) parsers.get(i)).toString(parsed.getBigDecimal(field));
                    }
                    else
                    {
                        values[i] = null;
                    }
                }
                else if (type instanceof FlatDataDecimal)
                {
                    if (parsed.hasDoubleValue(field))
                    {
                        values[i] = ((DecimalParser) parsers.get(i)).toString(parsed.getDouble(field));
                    }
                    else if (parsed.hasBigDecimalValue(field))
                    {
                        values[i] = ((DecimalParser) parsers.get(i)).toString(parsed.getBigDecimal(field));
                    }
                    else
                    {
                        values[i] = null;
                    }
                }
                else if (type instanceof FlatDataDate)
                {
                    values[i] = parsed.hasLocalDateValue(field) ? ((DateParser) parsers.get(i)).toString(parsed.getLocalDate(field)) : null;
                }
                else if (type instanceof FlatDataDateTime)
                {
                    values[i] = parsed.hasInstantValue(field) ? ((DateTimeParser) parsers.get(i)).toString(parsed.getInstant(field)) : null;
                }
                else
                {
                    throw new IllegalArgumentException("Unknown datatype: " + type.getClass().getSimpleName());
                }

                if (values[i] == null && !type.isOptional())
                {
                    throw new IllegalStateException("No value found for mandatory field '" + field.getLabel() + "'");
                }
            }
            writeLine(writer, values, lineNumber);
        }
        writer.flush();
    }

    public void writeLine(PrintWriter writer, String[] values, IntegerVariable lineNumber)
    {
        if (lineNumber.increment() > 1)
        {
            writer.write(helper.eol == null ? "\n" : helper.eol);
        }
        for (int i = 0; i < values.length; i++)
        {
            if (i > 0)
            {
                writer.write(helper.delimiter);
            }
            writer.write(values[i] == null ? nullString : values[i]);
        }
    }
}
