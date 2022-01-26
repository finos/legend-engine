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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.StreamingDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataDefect;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

abstract class AbstractBloombergDriverDescription extends StreamingDriverDescription implements FlatDataValidator
{
    static final String FILTER_PROPERTY = "filter";

    @Override
    public List<FlatDataVariable> getDeclares()
    {
        List<FlatDataVariable> result = new ArrayList<>(super.getDeclares());
        result.add(BloombergKeyValues.VARIABLE_LAST_METADATA);
        return result;
    }

    @Override
    public boolean isSelfDescribing()
    {
        return true;
    }

    @Override
    public <T> Function<FlatData, FlatDataProcessor.Builder<T>> getProcessorBuilderFactory()
    {
        return BloombergProcessor::newBuilder;
    }

    @Override
    public List<FlatDataDefect> validate(FlatData flatData, FlatDataSection section)
    {
        List<FlatDataDefect> defects = new ArrayList<>();
        List<String> allowed = Arrays.asList(
                BloombergDataDriverDescription.ID,
                BloombergMetadataDriverDescription.ID,
                BloombergActionsDriverDescription.ID,
                BloombergExtendActionDriverDescription.ID);
        if (!flatData.getSections().stream().map(FlatDataSection::getDriverId).allMatch(allowed::contains))
        {
            defects.add(new FlatDataDefect(flatData, section, "Bloomberg section must not be combined with non-Bloomberg sections"));
        }

        for (String filter: FlatDataUtils.getStrings(section.getSectionProperties(), FILTER_PROPERTY).orElse(Collections.emptyList()))
        {
            if (!filter.matches("[^=]+=[^=]+"))
            {
                defects.add(new FlatDataDefect(flatData, section, "Invalid filter value '" + filter + "' (Expected KEY=VALUE)"));
            }
        }
        return defects;
    }
}
