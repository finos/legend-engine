package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.StreamingDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RecordTypeMultiplicity;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataDefect;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidator;

import java.util.Collections;
import java.util.List;

public class BloombergCorporateActionsFileDriverDescription extends StreamingDriverDescription implements FlatDataValidator
{
    @Override
    public String getId()
    {
        return BloombergCorporateActionsFileReadDriver.ID;
    }

    @Override
    public FlatDataReadDriver newReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        if (!(context.getConnection() instanceof InputStreamConnection))
        {
            throw new RuntimeException("Invalid connection type for this driver");
        }
        return new BloombergCorporateActionsFileReadDriver(section, context);
    }

    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder(FlatDataUtils.dataTypeParsingProperties())
                .booleanProperty(BloombergCorporateActionsFileReadDriver.INCLUDE_NO_ACTION_RECORDS)
                .build();
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
