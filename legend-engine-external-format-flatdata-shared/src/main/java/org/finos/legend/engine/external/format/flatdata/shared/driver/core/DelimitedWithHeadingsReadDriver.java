package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.AbstractRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.NoValuesRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DelimitedWithHeadingsReadDriver<T> extends DelimitedReadDriver<T>
{
    static final String MODELLED_COUMNNS_REQIURED = "modelledColumnsMustBePresent";
    static final String ONLY_MODELLED_COLUMNS = "onlyModelledColumnsAllowed";

    private final FlatDataSection section;
    private final FlatDataRecordType recordType;
    private final FlatDataProcessingContext context;
    private final List<IDefect> headingDefects = new ArrayList<>();
    private long recordNumber = 0;
    private List<String> headings = null;
    private IChecked<RawFlatData> headingsLine = null;

    DelimitedWithHeadingsReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);
        this.section = section;
        this.context = context;
        this.recordType = Objects.requireNonNull(section.getRecordType());
    }

    @Override
    public String getId()
    {
        return DelimitedWithHeadingsDriverDescription.ID;
    }

    @Override
    public void start()
    {
        super.start();
        // First read will establish headings or establish that they are invalid
        headingsLine = readDelimitedLine().orElseGet(() -> BasicChecked.newChecked(createInvalidFlatDataDataRecord(null), null, BasicDefect.newInvalidInputCriticalDefect("Header row is missing.", context.getDefiningPath())));

        boolean headingsRequired = FlatDataUtils.getBoolean(section.getSectionProperties(), MODELLED_COUMNNS_REQIURED);
        boolean onlyModelled = FlatDataUtils.getBoolean(section.getSectionProperties(), ONLY_MODELLED_COLUMNS);

        if (headingsLine.getValue() == null || !headingsLine.getDefects().isEmpty() || headings == null)
        {
            headingDefects.addAll(headingsLine.getDefects());
            headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Header row is invalid. Skipping all data in this section.", context.getDefiningPath()));
        }
        else
        {
            for (FlatDataRecordField field : recordType.getFields())
            {
                if (headingsRequired && !headings.contains(field.getLabel()))
                {
                    headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Heading " + field.getLabel() + " missing for required column", context.getDefiningPath()));
                }
                else if (!field.isOptional() && !headings.contains(field.getLabel()))
                {
                    headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Heading " + field.getLabel() + " missing for mandatory column", context.getDefiningPath()));
                }
            }
            if (onlyModelled)
            {
                for (String heading : headings)
                {
                    if (recordType.getFields().stream().noneMatch(field -> heading.equals(field.getLabel())))
                    {
                        headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Unexpected heading " + heading, context.getDefiningPath()));
                    }
                }
            }
            if (!headingDefects.isEmpty())
            {
                headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Header row is invalid. Skipping all data in this section.", context.getDefiningPath()));
            }
        }

        this.fieldHandlers = computeFieldHandlers(recordType, this::getRawDataAccessor);
        this.objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(section.getRecordType(), fieldHandlers));
    }

    private Function<RawFlatData, String> getRawDataAccessor(FlatDataRecordField field)
    {
        int index = headings.indexOf(field.getLabel());
        if (index == -1)
        {
            return (RawFlatData raw) -> null;
        }
        else
        {
            return (RawFlatData raw) ->
            {
                String value = ((DelimitedWithHeadingsRawFlatData) raw).getRawValue(index);
                return value != null && helper.nullStrings.contains(value) ? null : value;
            };
        }
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        if (!headingDefects.isEmpty())
        {
            while (!isFinished())
            {
                readDelimitedLine();
            }
            return Collections.singletonList(BasicChecked.newChecked(null, headingsLine.getValue(), headingDefects));
        }

        return readDelimitedLine()
                .flatMap(this::makeParsed)
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    @Override
    protected RawFlatData createFlatDataDataRecord(LineReader.Line line, List<String> values)
    {
        long recNo;
        if (headings == null)
        {
            headings = values;
            recNo = 0;
        }
        else
        {
            recNo = ++recordNumber;
        }
        return new DelimitedWithHeadingsRawFlatData(recNo, line.getLineNumber(), line.getText(), values);
    }

    @Override
    protected RawFlatData createInvalidFlatDataDataRecord(LineReader.Line line)
    {
        long recNo = headings == null ? 0 : ++recordNumber;
        return new NoValuesRawFlatData(recNo, line.getLineNumber(), line.getText());
    }

    private class DelimitedWithHeadingsRawFlatData extends AbstractRawFlatData
    {
        private List<String> values;

        DelimitedWithHeadingsRawFlatData(long number, long lineNumber, String record, List<String> values)
        {
            super(number, lineNumber, record);
            this.values = values;
        }

        @Override
        protected List<RawFlatDataValue> createValues()
        {
            int limit = Math.min(headings.size(), values.size());
            return IntStream.range(0, limit).mapToObj(WithHeadingValue::new).collect(Collectors.toList());
        }

        String getRawValue(int index)
        {
            return index >= values.size() ? null : values.get(index);
        }

        private class WithHeadingValue implements RawFlatDataValue
        {
            private final int index;

            WithHeadingValue(int index)
            {
                this.index = index;
            }

            @Override
            public Object getAddress()
            {
                return headings.get(index);
            }

            @Override
            public String getRawValue()
            {
                return values.get(index);
            }

            @Override
            public String toString()
            {
                return "WithHeadingValue{label=" + getAddress() + ", value=" + getRawValue() + '}';
            }
        }
    }

}
