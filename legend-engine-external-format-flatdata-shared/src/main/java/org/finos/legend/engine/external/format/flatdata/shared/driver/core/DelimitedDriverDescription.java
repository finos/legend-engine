package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RecordTypeMultiplicity;

import java.util.List;

public abstract class DelimitedDriverDescription extends StreamingDriverDescription
{
    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder(super.getSectionProperties())
                .requiredStringProperty(DelimitedReadDriver.DELIMITER)
                .optionalStringProperty(DelimitedReadDriver.QUOTE_CHAR)
                .optionalStringProperty(DelimitedReadDriver.ESCAPING_CHAR)
                .optionalRepeatableStringProperty(DelimitedReadDriver.NULL_STRING)
                .build();
    }

    @Override
    public RecordTypeMultiplicity getRecordTypeMultiplicity()
    {
        return RecordTypeMultiplicity.MANDATORY;
    }
}
