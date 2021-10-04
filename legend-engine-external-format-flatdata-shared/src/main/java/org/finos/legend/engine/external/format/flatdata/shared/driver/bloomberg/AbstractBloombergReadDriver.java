package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.StreamingDriverHelper;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.StreamingReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLineReader;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.BasicRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.StringVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.function.LongSupplier;

abstract class AbstractBloombergReadDriver<T> extends StreamingReadDriver<T>
{
    static final String START_OF_FILE = "START-OF-FILE";
    static final String END_OF_FILE = "END-OF-FILE";
    static final String START_OF_FIELDS = "START-OF-FIELDS";
    static final String END_OF_FIELDS = "END-OF-FIELDS";
    static final String START_OF_DATA = "START-OF-DATA";
    static final String END_OF_DATA = "END-OF-DATA";
    static final String COMMENT_PREFIX = "##";
    static final String NULL_STRING = "N.A.";
    static final String FIELD_SEPARATOR_REGEXP = "\\|";

    private static final String BLOOMBERG_DATE_FORMAT_HEADER_KEY = "DATEFORMAT";

    private final FlatDataProcessingContext context;
    private final FlatDataSection section;
    private final StringVariable defaultStrictDateFormat;
    private final StringVariable defaultFalseString;
    private final StringVariable defaultTrueString;
    private boolean finished = false;
    private static ThreadLocal<BloombergKeyValues> metadata;

    AbstractBloombergReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(new StreamingDriverHelper(addAutomaticProperties(section), context));
        this.section = section;
        this.context = context;
        this.defaultStrictDateFormat = StringVariable.reference(context, FlatDataUtils.VARIABLE_DEFAULT_DATE_FORMAT);
        this.defaultTrueString = StringVariable.reference(context, FlatDataUtils.VARIABLE_DEFAULT_TRUE_STRING);
        this.defaultFalseString = StringVariable.reference(context, FlatDataUtils.VARIABLE_DEFAULT_FALSE_STRING);
    }

    private static FlatDataSection addAutomaticProperties(FlatDataSection section)
    {
        ArrayList<FlatDataProperty> newProperties = new ArrayList<>(section.getSectionProperties());
        if (!FlatDataUtils.getString(newProperties, StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS).isPresent())
        {
            FlatDataUtils.setString(END_OF_FILE, newProperties, StreamingDriverHelper.SCOPE, StreamingDriverHelper.UNTIL_LINE_EQUALS);
        }
        return section.setSectionProperties(newProperties);
    }

    @Override
    protected LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier)
    {
        return new SimpleLineReader(cursor, helper.eol, lineNumberSupplier);
    }

    @Override
    public void start()
    {
        super.start();
        metadata = ThreadLocal.withInitial(() -> new BloombergKeyValues(context));
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }

    // TODO
//    CheckedParsedDataFactory factory()
//    {
//        return new CheckedParsedDataFactory(context, section, section.recordType, this::getRawDataAccessor).withNullStrings(NULL_STRING, " ");
//    }

    protected Function<RawFlatData, String> getRawDataAccessor(FlatDataRecordField field)
    {
        return (RawFlatData raw) -> ((BasicRawFlatData) raw).getRawValue(field.getLabel());
    }

    private void parseMetadata(LineReader.Line line)
    {
        metadata.get().parse(line);
    }

    void findStartOfFile()
    {
        try
        {
            untilLine(l -> START_OF_FILE.equals(l.getText()), NO_OP);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to find " + START_OF_FILE, e);
        }
    }

    void parseMetadataUntil(String stopAt)
    {

        try
        {
            untilLine(l -> stopAt.equals(l.getText()), this::parseMetadata);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to find " + stopAt, e);
        }
    }

    Collection<IChecked<T>> finish()
    {
        try
        {
            untilLine(l -> END_OF_FILE.equals(l.getText()), this::parseMetadata);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Unable to find " + END_OF_FILE, e);
        }

        finished = true;
        return Collections.emptyList();
    }

//    Collection<IChecked<ParsedFlatData>> retrieveMetadata(CheckedParsedDataFactory metadataFactory)
//    {
//        finished = true;
//        if (metadataFactory != null)
//        {
//            Optional<IChecked<ParsedFlatData>> metadataRow = metadataFactory.make(metadata.get().toCheckedFlatDataRecord());
//            return metadataRow.map(Collections::singletonList).orElseGet(Collections::emptyList);
//        }
//        else
//        {
//            return Collections.emptyList();
//        }
//    }

    void setupParsing()
    {
        BloombergKeyValues bloombergKeyValues = metadata.get();
        if (bloombergKeyValues.containsKey(BLOOMBERG_DATE_FORMAT_HEADER_KEY))
        {
            defaultStrictDateFormat.set(bloombergKeyValues.get(BLOOMBERG_DATE_FORMAT_HEADER_KEY).replace('m', 'M'));
        }
        defaultTrueString.set("Y");
        defaultFalseString.set("N");
    }
}
