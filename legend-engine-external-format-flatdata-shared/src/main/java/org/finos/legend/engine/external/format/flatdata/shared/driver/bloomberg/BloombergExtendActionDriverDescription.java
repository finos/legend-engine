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

package org.finos.legend.engine.external.format.flatdata.shared.driver.bloomberg;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.CommonDataHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.*;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataDefect;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/* Driver to extend Bloomberg getaction line for deletes */
public class BloombergExtendActionDriverDescription implements FlatDataDriverDescription, FlatDataValidator
{
    public static final String ID = "BloombergActionDetails";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public <T> FlatDataReadDriver<T> newReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        return new BloombergExtendActionReadDriver<>(section, context);
    }

    @Override
    public List<FlatDataVariable> getDeclares()
    {
        List<FlatDataVariable> result = new ArrayList<>(CommonDataHandler.dataTypeParsingVariables());
        result.add(BloombergActionsReadDriver.VARIABLE_ACTIONS_RECORD);
        return result;
    }

    @Override
    public List<PropertyDescription> getSectionProperties()
    {
        return new PropertyDescription.Builder()
                .requiredRepeatableStringProperty(BloombergExtendActionReadDriver.ACTION_FLAGS)
                .optionalRepeatableStringProperty(BloombergExtendActionReadDriver.MNEMONICS)
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
    public List<FlatDataDefect> validate(FlatData flatData, FlatDataSection section)
    {
        List<FlatDataDefect> defects = new ArrayList<>();
        List<String> skipablePredecessors = Arrays.asList(BloombergMetadataDriverDescription.ID, ID);
        List<FlatDataSection> sections = flatData.getSections();
        FlatDataSection predecessor = null;
        for (int i = sections.indexOf(section) -1; predecessor == null && i>=0; i--)
        {
            if (!skipablePredecessors.contains(sections.get(i).getDriverId()))
            {
                predecessor = sections.get(i);
            }
        }
        if (predecessor == null || !predecessor.getDriverId().equals(BloombergActionsDriverDescription.ID))
        {
            defects.add(new FlatDataDefect(flatData, section, ID + " sections must follow a " + BloombergActionsDriverDescription.ID + " section"));
        }
        return defects;
    }

    @Override
    public <T> Function<FlatData, FlatDataProcessor.Builder<T>> getProcessorBuilderFactory()
    {
        return BloombergProcessor::newBuilder;
    }
}
