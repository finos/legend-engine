package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BasicRawFlatData extends AbstractRawFlatData
{
    private final List<String> headings;
    private final List<String> values;
    private List<RawFlatDataValue> recordValues = null;

    private BasicRawFlatData(long number, long lineNumber, String record, List<String> headings, List<String> values)
    {
        super(number, lineNumber, record);
        this.headings = headings;
        this.values = values;
    }

    public String getRawValue(Object address)
    {
        int index = (address instanceof String)
                ? headings.indexOf(address)
                : ((Number) address).intValue() - 1;
        return index == -1 || index >= values.size() ? null : values.get(index);
    }

    @Override
    protected List<RawFlatDataValue> createValues()
    {
        if (headings == null)
        {
            return IntStream.range(0, values.size()).mapToObj(WithoutHeadingValue::new).collect(Collectors.toList());
        }
        else
        {
            int limit = Math.min(headings.size(), values.size());
            return IntStream.range(0, limit).mapToObj(WithHeadingValue::new).collect(Collectors.toList());
        }
    }

    public static RawFlatData newRecord(long recordNumber, long lineNumber, String record, List<String> rawValues)
    {
        return new BasicRawFlatData(recordNumber, lineNumber, record, null, rawValues);
    }

    public static RawFlatData newRecord(long recordNumber, long lineNumber, String record, List<String> headings, List<String> rawValues)
    {
        return new BasicRawFlatData(recordNumber, lineNumber, record, headings, rawValues);
    }

    @Override
    public String toString()
    {
        return "BasicRawFlatData{" +
                "number=" + getNumber() +
                ", lineNumber=" + getLineNumber() +
                ", record='" + getRecord() + '\'' +
                ", recordValues=" + getRecordValues() +
                '}';
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
            return "RawFlatDataValue{label=" + getAddress() + ", value=" + getRawValue() +'}';
        }
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
            return "RawFlatDataValue{label=" + getAddress() + ", value=" + getRawValue() +'}';
        }
    }
}
