package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;

import java.util.ArrayList;
import java.util.List;

public abstract class StreamingDriverDescription implements FlatDataDriverDescription
{
    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder(FlatDataUtils.dataTypeParsingProperties())
                .optionalStringProperty(StreamingDriverHelper.RECORD_SEPARATOR)
                .booleanProperty(StreamingDriverHelper.MAY_CONTAIN_BLANK_LINES)
                .requiredExclusiveGroup(StreamingDriverHelper.SCOPE, b->b
                        .booleanProperty(StreamingDriverHelper.UNTIL_EOF)
                        .optionalIntegerProperty(StreamingDriverHelper.FOR_NUMBER_OF_LINES)
                        .optionalStringProperty(StreamingDriverHelper.UNTIL_LINE_EQUALS)
                        .booleanProperty(StreamingDriverHelper.DEFAULT)
                )
                .build();
    }

    @Override
    public List<FlatDataVariable> getDeclares()
    {
        List<FlatDataVariable> result = new ArrayList<>(FlatDataUtils.dataTypeParsingVariables());
        result.add(StreamingDriverHelper.VARIABLE_LINE_NUMBER);
        return result;
    }
}
