package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;

import java.util.Collections;
import java.util.List;

public class NoValuesRawFlatData extends AbstractRawFlatData
{
    public NoValuesRawFlatData(long number, long lineNumber, String record)
    {
        super(number, lineNumber, record);
    }

    @Override
    protected List<RawFlatDataValue> createValues()
    {
        return Collections.emptyList();
    }
}
