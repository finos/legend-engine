package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;

import java.util.List;

public abstract class AbstractRawFlatData implements RawFlatData
{
    private final long number;
    private final long lineNumber;
    private final String record;
    private List<RawFlatDataValue> recordValues = null;

    protected AbstractRawFlatData(long number, long lineNumber, String record)
    {
        this.number = number;
        this.lineNumber = lineNumber;
        this.record = record;
    }

    public long getNumber()
    {
        return number;
    }

    public long getLineNumber()
    {
        return lineNumber;
    }

    public String getRecord()
    {
        return record;
    }

    public List<RawFlatDataValue> getRecordValues()
    {
        if (recordValues == null)
        {
            recordValues = createValues();
        }
        return recordValues;
    }

    protected abstract List<RawFlatDataValue> createValues();

    @Override
    public String toString()
    {
        return "BasicRawFlatData{" +
                "number=" + number +
                ", lineNumber=" + lineNumber +
                ", record='" + record + '\'' +
                ", recordValues=" + getRecordValues() +
                '}';
    }
}
