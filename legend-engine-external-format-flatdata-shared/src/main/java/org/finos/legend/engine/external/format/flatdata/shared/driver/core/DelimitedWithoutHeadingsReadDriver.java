package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.AbstractRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.NoValuesRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DelimitedWithoutHeadingsReadDriver<T> extends DelimitedReadDriver<T>
{
    public static final String ID = "DelimitedWithoutHeadings";
    private final FlatDataProcessingContext context;

    private long recordNumber = 0;

    DelimitedWithoutHeadingsReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);
        this.context = context;
    }

    @Override
    public void start()
    {
        super.start();
        this.fieldHandlers = computeFieldHandlers(Objects.requireNonNull(helper.section.getRecordType()), this::getRawDataAccessor);
        this.objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(helper.section.getRecordType(), fieldHandlers));
    }

    private Function<RawFlatData, String> getRawDataAccessor(FlatDataRecordField field)
    {
        int index = Integer.parseInt(field.getAddress()) - 1;
        return (RawFlatData raw) -> ((DelimitedWithoutHeadingsRawFlatData) raw).getRawValue(index);
    }

    @Override
    public String getId()
    {
        return DelimitedWithoutHeadingsReadDriver.ID;
    }


    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        return readDelimitedLine()
                .flatMap(this::makeParsed)
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    @Override
    protected RawFlatData createFlatDataDataRecord(LineReader.Line line, List<String> values)
    {
        return new DelimitedWithoutHeadingsRawFlatData(++recordNumber, line.getLineNumber(), line.getText(), values);
    }

    @Override
    protected RawFlatData createInvalidFlatDataDataRecord(LineReader.Line line)
    {
        return new NoValuesRawFlatData(++recordNumber, line.getLineNumber(), line.getText());
    }

    private static class DelimitedWithoutHeadingsRawFlatData extends AbstractRawFlatData
    {
        private List<String> values;

        DelimitedWithoutHeadingsRawFlatData(long number, long lineNumber, String record, List<String> values)
        {
            super(number, lineNumber, record);
            this.values = values;
        }

        @Override
        protected List<RawFlatDataValue> createValues()
        {
            return IntStream.range(0, values.size()).mapToObj(WithoutHeadingValue::new).collect(Collectors.toList());
        }

        String getRawValue(int index)
        {
            return index < 0 || index >= values.size() ? null : values.get(index);
        }

        private class WithoutHeadingValue implements RawFlatDataValue
        {
            private final int index;

            WithoutHeadingValue(int index)
            {
                this.index = index;
            }

            @Override
            public Object getAddress()
            {
                return index + 1L;
            }

            @Override
            public String getRawValue()
            {
                return values.get(index);
            }

            @Override
            public String toString()
            {
                return "WithoutHeadingValue{label=" + getAddress() + ", value=" + getRawValue() +'}';
            }
        }
    }
}
