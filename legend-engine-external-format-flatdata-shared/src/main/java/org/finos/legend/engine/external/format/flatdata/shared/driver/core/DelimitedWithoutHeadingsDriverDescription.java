package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.ObjectStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataWriteDriver;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataDefect;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidator;

import java.util.ArrayList;
import java.util.List;

public class DelimitedWithoutHeadingsDriverDescription extends DelimitedDriverDescription implements FlatDataValidator
{
    @Override
    public String getId()
    {
        return DelimitedWithoutHeadingsReadDriver.ID;
    }

    @Override
    public FlatDataReadDriver newReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        if (!(context.getConnection() instanceof InputStreamConnection))
        {
            throw new RuntimeException("Invalid connection type for this driver");
        }
        return new DelimitedWithoutHeadingsReadDriver(section, context);
    }

    @Override
    public <T> FlatDataWriteDriver<T> newWriteDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        if (!(context.getConnection() instanceof ObjectStreamConnection))
        {
            throw new RuntimeException("Invalid connection type for this driver");
        }
        return new DelimitedWithoutHeadingsWriteDriver(section, context);
    }

    @Override
    public List<FlatDataDefect> validate(FlatData store, FlatDataSection section)
    {
        List<FlatDataDefect> defects = new ArrayList<>();
        section.getRecordType().getFields().forEach(field ->
        {
            if (field.getAddress() != null && !field.getAddress().matches("\\d+"))
            {
                defects.add(new FlatDataDefect(store, section, "Invalid address for '" + field.getLabel() + "' (Expected column number)"));
            }
        });
        return defects;
    }

    @Override
    public boolean isSelfDescribing()
    {
        return false;
    }
}
