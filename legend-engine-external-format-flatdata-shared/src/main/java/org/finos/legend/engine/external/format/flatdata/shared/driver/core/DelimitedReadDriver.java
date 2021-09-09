package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.LazyParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.DelimitedLine;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.EnforcementLevel;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.LongSupplier;

public abstract class DelimitedReadDriver<T> extends StreamingReadDriver<T>
{
    static final String DELIMITER = "delimiter";
    static final String QUOTE_CHAR = "quoteChar";
    static final String ESCAPING_CHAR = "escapingChar";
    static final String NULL_STRING = "nullString";

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    protected final DelimitedDriverHelper helper;

    protected ParsedFlatDataToObject<? extends T> objecFactory;
    protected List<FieldHandler> fieldHandlers;

    DelimitedReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(new DelimitedDriverHelper(section, context));
        helper = (DelimitedDriverHelper) super.helper;
    }

    @Override
    protected LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier)
    {
        return new DelimitedLineReader(cursor, helper.eol, helper.context.getDefiningPath(), lineNumberSupplier, helper.delimiter, helper.quoteChar, helper.escapeChar);
    }

    protected Optional<IChecked<RawFlatData>> readDelimitedLine()
    {
        DelimitedLine line = (DelimitedLine) nextLine();
        if (helper.skipBlankLines && line.isEmpty())
        {
            return Optional.empty();
        }
        else if (line.getDefects().isEmpty())
        {
            return Optional.of(BasicChecked.newChecked(createFlatDataDataRecord(line, line.getValues()), null));
        }
        else
        {
            return Optional.of(BasicChecked.newChecked(createInvalidFlatDataDataRecord(line), null, line.getDefects()));
        }
    }

    Optional<IChecked<T>> makeParsed(IChecked<RawFlatData> unparsed)
    {
        if (unparsed.getDefects().stream().anyMatch(d -> d.getEnforcementLevel() == EnforcementLevel.Critical))
        {
            return Optional.of(BasicChecked.newChecked(null, unparsed.getValue(), unparsed.getDefects()));
        }
        else if (unparsed.getValue() == null)
        {
            return Optional.empty();
        }
        else
        {
            RawFlatData rawData = unparsed.getValue();
            LazyParsedFlatData parseData = new LazyParsedFlatData(rawData, unparsed.getDefects(), fieldHandlers, helper.context.getDefiningPath());

            for (FieldHandler handler : fieldHandlers)
            {
                if (handler.hasRawValue(rawData))
                {
                    String errorMessage = handler.validate(rawData);
                    if (errorMessage == null)
                    {
                        parseData.setVerified(handler);
                    }
                    else
                    {
                        parseData.addInvalidInputDefect(handler, errorMessage);
                    }
                }
                else if (!handler.getField().isOptional())
                {
                    parseData.addMissingValueDefect(handler);
                }
                else
                {
                    parseData.setMissing(handler);
                }
            }
            T value = objecFactory.make(parseData);
            return Optional.of(BasicChecked.newChecked(value, unparsed.getValue(), parseData.getDefects()));
        }
    }

    protected abstract RawFlatData createFlatDataDataRecord(LineReader.Line line, List<String> values);

    protected abstract RawFlatData createInvalidFlatDataDataRecord(LineReader.Line line);
}
