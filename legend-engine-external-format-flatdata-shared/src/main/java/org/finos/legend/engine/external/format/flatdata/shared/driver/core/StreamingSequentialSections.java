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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.ObjectStreamConnection;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SectionProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ProcessingVariables;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.*;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class StreamingSequentialSections<T> implements FlatDataProcessor<T>
{
    private static final List<FlatDataDriverDescription> descriptions = FlatDataDriverDescription.loadAll();

    private final FlatData flatData;
    private final String definingPath;
    private final Map<String, Function<FlatDataRecordType, ParsedFlatDataToObject<?>>> toObjectFactoryFactories;
    private final Map<String, Function<FlatDataRecordType, ObjectToParsedFlatData<?>>> fromObjectFactoryFactories;

    private StreamingSequentialSections(FlatData flatData,
                                        String definingPath,
                                        Map<String, Function<FlatDataRecordType, ParsedFlatDataToObject<?>>> toObjectFactoryFactories,
                                        Map<String, Function<FlatDataRecordType, ObjectToParsedFlatData<?>>> fromObjectFactoryFactories)
    {
        this.flatData = flatData;
        this.definingPath = definingPath;
        this.toObjectFactoryFactories = toObjectFactoryFactories;
        this.fromObjectFactoryFactories = fromObjectFactoryFactories;
    }

    public void readData(InputStream inputStream, Consumer<IChecked<T>> consumer)
    {
        try
        {
            Connection connection = new InputStreamConnection(inputStream, StandardCharsets.UTF_8);
            connection.open();

            ProcessingVariables variables = new ProcessingVariables(flatData);
            List<FlatDataReadDriver<T>> drivers = new LinkedList<>();
            FlatDataReadDriver<T> drv = null;
            for (int i = flatData.getSections().size() - 1; i >= 0; i--)
            {
                FlatDataSection section = flatData.getSections().get(i);
                drv = descriptionFor(section).newReadDriver(section, sectionContext(section, connection, variables, drv));
                drivers.add(0, drv);
            }

            for (FlatDataReadDriver<T> driver: drivers)
            {
                driver.start();
                while (!driver.isFinished())
                {
                    driver.readCheckedObjects().forEach(consumer);
                }
                driver.stop();
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeData(Stream<T> inputStream, OutputStream outputStream)
    {
        try
        {
            Connection connection = new ObjectStreamConnection(inputStream);
            connection.open();

            ProcessingVariables variables = new ProcessingVariables(flatData);
            FlatDataWriteDriver<T> drv = null;
            List<FlatDataWriteDriver<T>> drivers = new LinkedList<>();
            for (int i = flatData.getSections().size() - 1; i >= 0; i--)
            {
                FlatDataSection section = flatData.getSections().get(i);
                drv = descriptionFor(section).newWriteDriver(section, sectionContext(section, connection, variables, drv));
                drivers.add(0, drv);
            }

            for (FlatDataWriteDriver<T> driver: drivers)
            {
                driver.write(outputStream);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private SectionProcessingContext sectionContext(FlatDataSection section, Connection connection, ProcessingVariables variables, FlatDataDriver nextDriver)
    {
        FlatDataDriverDescription description = descriptionFor(section);
        return new StreamingSequentialSectionsProcessingContext(connection,
                definingPath,
                description,
                section,
                toObjectFactoryFactories.get(section.getName()),
                fromObjectFactoryFactories.get(section.getName()),
                variables,
                nextDriver);
    }

    private FlatDataDriverDescription descriptionFor(FlatDataSection section)
    {
        return descriptions.stream()
                .filter(d -> d.getId().equals(section.getDriverId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No driver for: '" + section.getDriverId() + "'"));
    }

    public static class Builder<T> implements FlatDataProcessor.Builder<T>
    {
        private final FlatData flatData;
        private String definingPath = "unknown";
        private Map<String, Function<FlatDataRecordType, ParsedFlatDataToObject<?>>> toObjectFactoryFactories = new HashMap<>();
        private Map<String, Function<FlatDataRecordType, ObjectToParsedFlatData<?>>> fromObjectFactoryFactories = new HashMap<>();

        public Builder(FlatData flatData)
        {
            this.flatData = flatData;
        }

        @Override
        public FlatDataProcessor.Builder<T> withDefiningPath(String definingPath)
        {
            this.definingPath = definingPath;
            return this;
        }

        @Override
        public FlatDataProcessor.Builder<T> withToObjectFactoryFactory(String sectionId, Function<FlatDataRecordType, ParsedFlatDataToObject<?>> toObjectFactoryFactory)
        {
            toObjectFactoryFactories.put(sectionId, toObjectFactoryFactory);
            return this;
        }

        @Override
        public FlatDataProcessor.Builder<T> withFromObjectFactoryFactory(String sectionId, Function<FlatDataRecordType, ObjectToParsedFlatData<?>> fromObjectFactoryFactory)
        {
            fromObjectFactoryFactories.put(sectionId, fromObjectFactoryFactory);
            return this;
        }

        @Override
        public FlatDataProcessor<T> build()
        {
            return new StreamingSequentialSections<>(flatData, definingPath, toObjectFactoryFactories, fromObjectFactoryFactories);
        }
    }

    public static <T> Builder<T> newBuilder(FlatData flatData)
    {
        return new Builder<>(flatData);
    }
}
