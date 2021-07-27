package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.StreamingDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RecordTypeMultiplicity;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataDefect;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidator;

import java.util.Collections;
import java.util.List;

public class BloombergMetadataDriverDescription extends StreamingDriverDescription implements FlatDataValidator
{
    @Override
    public String getId()
    {
        return BloombergMetadataReadDriver.ID;
    }

    @Override
    public FlatDataReadDriver newReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        if (!(context.getConnection() instanceof InputStreamConnection))
        {
            throw new RuntimeException("Invalid connection type for this driver");
        }
        return new BloombergMetadataReadDriver(section, context);
    }

    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return Collections.emptyList();
    }

    @Override
    public RecordTypeMultiplicity getRecordTypeMultiplicity()
    {
        return RecordTypeMultiplicity.MANDATORY;
    }

    @Override
    public boolean isSelfDescribing()
    {
        return true;
    }

    @Override
    public List<FlatDataDefect> validate(FlatData store, FlatDataSection section)
    {
        return Collections.emptyList();
    }
}
