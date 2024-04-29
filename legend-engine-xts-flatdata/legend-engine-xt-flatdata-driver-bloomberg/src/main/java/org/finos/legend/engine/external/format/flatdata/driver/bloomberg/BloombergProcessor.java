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

package org.finos.legend.engine.external.format.flatdata.driver.bloomberg;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.external.format.flatdata.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.driver.core.connection.InputStreamConnection;
import org.finos.legend.engine.external.format.flatdata.driver.core.util.SectionProcessingContext;
import org.finos.legend.engine.external.format.flatdata.driver.core.variables.ProcessingVariables;
import org.finos.legend.engine.external.format.flatdata.driver.spi.Connection;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataDriverDescription;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataProcessor;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ObjectToParsedFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public class BloombergProcessor<T> implements FlatDataProcessor<T>
{
    private static final List<FlatDataDriverDescription> descriptions = FlatDataDriverDescription.loadAll();

    private final FlatData flatData;
    private final String definingPath;
    private final T schemaObject;
    private final Map<String, Function<FlatDataRecordType, ParsedFlatDataToObject<?>>> toObjectFactoryFactories;
    private final Map<String, Function<FlatDataRecordType, ObjectToParsedFlatData<?>>> fromObjectFactoryFactories;
    private final ArrayList<IChecked<T>> defectiveObjects = new ArrayList<>();

    private BloombergProcessor(FlatData flatData,
                               String definingPath,
                                T schemaObject,
                               Map<String, Function<FlatDataRecordType, ParsedFlatDataToObject<?>>> toObjectFactoryFactories,
                               Map<String, Function<FlatDataRecordType, ObjectToParsedFlatData<?>>> fromObjectFactoryFactories)
    {
        this.flatData = flatData;
        this.definingPath = definingPath;
        this.schemaObject = schemaObject;
        this.toObjectFactoryFactories = toObjectFactoryFactories;
        this.fromObjectFactoryFactories = fromObjectFactoryFactories;
    }

    public void readData(InputStream inputStream, Consumer<IChecked<T>> consumer)
    {
        try
        {
            InputStreamConnection connection = new InputStreamConnection(inputStream);
            connection.open();

            Consumer<IChecked<T>> sectionConsumer = consumer;
            if (this.schemaObject != null)
            {
                sectionConsumer = this::enqueueDefectiveObjects;
            }

            ProcessingVariables variables = new ProcessingVariables(flatData);
            List<BloombergSection> bloombergSections = new ArrayList<>();
            for (FlatDataSection section : flatData.sections)
            {
                if (section.driverId.equals(BloombergDataDriverDescription.ID))
                {
                    bloombergSections.add(new DataSection(section, connection, variables));
                }
                else if (section.driverId.equals(BloombergActionsDriverDescription.ID))
                {
                    bloombergSections.add(new ActionsSection(section, connection, variables));
                }
                else if (section.driverId.equals(BloombergExtendActionDriverDescription.ID))
                {
                    ((ActionsSection) bloombergSections.get(bloombergSections.size() - 1)).addActionSection(section);
                }
                else if (section.driverId.equals(BloombergMetadataDriverDescription.ID))
                {
                    bloombergSections.get(bloombergSections.size() - 1).addMetadataSection(section);
                }
                else
                {
                    throw new IllegalStateException("Unknown Bloomberg driver type: " + section.driverId);
                }
            }

            CharCursor cursor = connection.getCursor();
            while (!cursor.isEndOfData())
            {
                BloombergSection bloombergSection = null;
                for (BloombergSection bs : bloombergSections)
                {
                    if (bs.canStartAt(cursor))
                    {
                        bloombergSection = bs;
                        break;
                    }
                }
                if (bloombergSection == null)
                {
                    AbstractBloombergReadDriver.skipSection(cursor);
                }
                else
                {
                    bloombergSection.process(sectionConsumer);
                }
            }

            bloombergSections.forEach(BloombergSection::checkProcessed);

            if (this.schemaObject != null)
            {
                if (!this.defectiveObjects.isEmpty())
                {
                    MutableList<String> defectMessages = ListIterate.flatCollect(this.defectiveObjects, IChecked::getDefects).collect(IDefect::getMessage);
                    // it is safe to throw error as we don't allow checked queries on schema
                    throw new IllegalStateException(String.format("Defects found: %s", defectMessages.makeString()));
                }
                consumer.accept(BasicChecked.newChecked(this.schemaObject, null));
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
        throw new UnsupportedOperationException("Write not supported for Bloomberg");
    }

    private void enqueueDefectiveObjects(IChecked<T> object)
    {
        this.defectiveObjects.add(object);
    }

    private FlatDataDriverDescription descriptionFor(FlatDataSection section)
    {
        return descriptions.stream()
                .filter(d -> d.getId().equals(section.driverId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No driver for: '" + section.driverId + "'"));
    }

    private abstract class BloombergSection
    {
        final FlatDataSection section;
        final Connection connection;
        final ProcessingVariables variables;
        boolean processed = false;
        FlatDataSection metadataSection = null;
        BloombergMetadataReadDriver<T> metadataReadDriver;

        BloombergSection(FlatDataSection section, Connection connection, ProcessingVariables variables)
        {
            this.section = section;
            this.connection = connection;
            this.variables = variables;
        }

        void addMetadataSection(FlatDataSection metadataSection)
        {
            this.metadataSection = metadataSection;
            FlatDataDriverDescription description = descriptionFor(metadataSection);
            SectionProcessingContext context = new SectionProcessingContext(connection,
                    definingPath,
                    description,
                    metadataSection,
                    toObjectFactoryFactories.get(metadataSection.name),
                    fromObjectFactoryFactories.get(metadataSection.name),
                    variables);
            this.metadataReadDriver = (BloombergMetadataReadDriver<T>) description.newReadDriver(metadataSection, context);
        }

        abstract AbstractBloombergReadDriver<T> readDriver();

        boolean canStartAt(CharCursor cursor)
        {
            return !processed && readDriver().canStartAt(cursor);
        }

        void process(Consumer<IChecked<T>> consumer)
        {
            doRead(readDriver(), consumer);

            if (metadataSection != null)
            {
                doRead(metadataReadDriver, consumer);
            }
            processed = true;
        }

        private void doRead(FlatDataReadDriver<T> driver, Consumer<IChecked<T>> consumer)
        {
            driver.start();
            while (!driver.isFinished())
            {
                driver.readCheckedObjects().forEach(consumer);
            }
            driver.stop();
        }

        void checkProcessed()
        {
            if (readDriver().isMandatory() && !processed)
            {
                throw new IllegalStateException("Mandatory section " + section.name + " missing");
            }
        }
    }

    private class DataSection extends BloombergSection
    {
        private final BloombergDataReadDriver<T> readDriver;

        DataSection(FlatDataSection section, Connection connection, ProcessingVariables variables)
        {
            super(section, connection, variables);

            FlatDataDriverDescription description = descriptionFor(section);
            SectionProcessingContext context = new SectionProcessingContext(connection,
                    definingPath,
                    description,
                    section,
                    toObjectFactoryFactories.get(section.name),
                    fromObjectFactoryFactories.get(section.name),
                    variables);
            this.readDriver = (BloombergDataReadDriver<T>) description.newReadDriver(section, context);
        }

        @Override
        AbstractBloombergReadDriver<T> readDriver()
        {
            return readDriver;
        }
    }

    private class ActionsSection extends BloombergSection
    {
        private final BloombergActionsReadDriver<T> readDriver;

        ActionsSection(FlatDataSection section, Connection connection, ProcessingVariables variables)
        {
            super(section, connection, variables);

            FlatDataDriverDescription description = descriptionFor(section);
            SectionProcessingContext context = new SectionProcessingContext(connection,
                    definingPath,
                    description,
                    section,
                    toObjectFactoryFactories.get(section.name),
                    fromObjectFactoryFactories.get(section.name),
                    variables);
            this.readDriver = (BloombergActionsReadDriver<T>) description.newReadDriver(section, context);
        }

        @Override
        AbstractBloombergReadDriver<T> readDriver()
        {
            return readDriver;
        }

        void addActionSection(FlatDataSection actionSection)
        {
            if (!this.section.driverId.equals(BloombergActionsDriverDescription.ID))
            {
                throw new IllegalStateException("Can only define a " + actionSection.driverId + " to a " + BloombergActionsDriverDescription.ID + " section");
            }
            FlatDataDriverDescription description = descriptionFor(actionSection);
            SectionProcessingContext context = new SectionProcessingContext(connection,
                    definingPath,
                    description,
                    actionSection,
                    toObjectFactoryFactories.get(actionSection.name),
                    fromObjectFactoryFactories.get(actionSection.name),
                    variables);
            BloombergExtendActionReadDriver<T> actionDriver = (BloombergExtendActionReadDriver<T>) description.newReadDriver(actionSection, context);
            readDriver.addActionDriver(actionDriver);
        }
    }

    public static class Builder<T> implements FlatDataProcessor.Builder<T>
    {
        private final FlatData flatData;
        private String definingPath = "unknown";
        private T schemaObject;
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
        public FlatDataProcessor.Builder<T> withSchemaObject(T schemaObject)
        {
            this.schemaObject = schemaObject;
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
            return new BloombergProcessor<>(flatData, definingPath, schemaObject, toObjectFactoryFactories, fromObjectFactoryFactories);
        }
    }

    static <T> Builder<T> newBuilder(FlatData flatData)
    {
        return new Builder<>(flatData);
    }
}
