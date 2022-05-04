// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.CommonDataHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.PropertyDescription;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class StreamingDriverDescription implements FlatDataDriverDescription
{
    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder(CommonDataHandler.dataTypeParsingProperties())
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
        List<FlatDataVariable> result = new ArrayList<>(CommonDataHandler.dataTypeParsingVariables());
        result.add(StreamingDriverHelper.VARIABLE_LINE_NUMBER);
        return result;
    }

    @Override
    public <T> Function<FlatData, FlatDataProcessor.Builder<T>> getProcessorBuilderFactory()
    {
        return StreamingSequentialSections::newBuilder;
    }
}
