//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.flatdata;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class FlatDataContext<T>
{
    private final List<FlatDataDriverDescription> descriptions = FlatDataDriverDescription.loadAll();

    private final FlatData flatData;
    private final String definingPath;
    private final Map<String, Function<FlatDataRecordType, ParsedFlatDataToObject<?>>> toObjectFactories = Maps.mutable.empty();
    private final Map<String, Function<FlatDataRecordType, ObjectToParsedFlatData<?>>> fromObjectFactories = Maps.mutable.empty();

    public FlatDataContext(FlatData flatData, String definingPath)
    {
        this.flatData = flatData;
        this.definingPath = definingPath;
    }

    public FlatDataContext<T> withSectionToObjectFactory(String sectionName, Function<FlatDataRecordType, ParsedFlatDataToObject<?>> factoryFactory)
    {
        toObjectFactories.put(sectionName, factoryFactory);
        return this;
    }

    public FlatDataContext<T> withSectionFromObjectFactory(String sectionName, Function<FlatDataRecordType, ObjectToParsedFlatData<?>> factoryFactory)
    {
        fromObjectFactories.put(sectionName, factoryFactory);
        return this;
    }

    public FlatDataProcessor<T> createProcessor()
    {
        FlatDataSection firstSection = flatData.getSections().get(0);
        FlatDataProcessor.Builder<T> builder = descriptionFor(firstSection).<T>getProcessorBuilderFactory().apply(flatData).withDefiningPath(definingPath);

        for (FlatDataSection section : flatData.getSections())
        {
            String sectionName = section.getName();
            if (toObjectFactories.containsKey(sectionName))
            {
                builder.withToObjectFactoryFactory(sectionName, toObjectFactories.get(sectionName));
            }
            else
            {
                builder.withToObjectFactoryFactory(sectionName, x ->
                {
                    throw new IllegalStateException("No to object factory provided for section " + sectionName);
                });
            }
            if (fromObjectFactories.containsKey(sectionName))
            {
                builder.withFromObjectFactoryFactory(sectionName, fromObjectFactories.get(sectionName));
            }
            else
            {
                builder.withFromObjectFactoryFactory(sectionName, x ->
                {
                    throw new IllegalStateException("No from object factory provided for section " + sectionName);
                });
            }
        }
        return builder.build();
    }

    private FlatDataDriverDescription descriptionFor(FlatDataSection section)
    {
        return descriptions.stream()
                .filter(d -> d.getId().equals(section.getDriverId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No driver for: '" + section.getDriverId() + "'"));
    }
}
