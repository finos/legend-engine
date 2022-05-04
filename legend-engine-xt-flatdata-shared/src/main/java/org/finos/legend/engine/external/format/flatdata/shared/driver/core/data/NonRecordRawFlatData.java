package org.finos.legend.engine.external.format.flatdata.shared.driver.core.data;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatDataValue;

import java.util.List;

public class NonRecordRawFlatData extends AbstractRawFlatData
{
    private final RawFlatData orginal;

    public NonRecordRawFlatData(RawFlatData orginal)
    {
        super(0, orginal.getLineNumber(), orginal.getRecord());
        this.orginal = orginal;
    }

    @Override
    protected List<RawFlatDataValue> createValues()
    {
        return orginal.getRecordValues();
    }
}
