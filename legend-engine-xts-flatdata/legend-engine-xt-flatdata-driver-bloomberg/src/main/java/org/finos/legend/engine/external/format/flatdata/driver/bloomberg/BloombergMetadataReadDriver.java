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

import org.finos.legend.engine.external.format.flatdata.driver.core.data.HeadedFlatDataFactory;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.driver.core.util.CommonDataHandler;
import org.finos.legend.engine.external.format.flatdata.driver.core.variables.ObjectVariable;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class BloombergMetadataReadDriver<T> implements FlatDataReadDriver<T>
{
    private FlatDataSection section;
    private final ObjectVariable<BloombergKeyValues> metadata;
    private final FlatDataProcessingContext context;
    private final CommonDataHandler commonDataHandler;
    private ParsedFlatDataToObject<? extends T> objectFactory;
    private List<FieldHandler> fieldHandlers;
    private HeadedFlatDataFactory<T> dataFactory;
    private BloombergKeyValues lastMetadata;
    private boolean finished = false;

    BloombergMetadataReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        this.section = section;
        this.metadata = ObjectVariable.reference(context, BloombergKeyValues.VARIABLE_LAST_METADATA);
        this.context = context;
        this.commonDataHandler = new CommonDataHandler(section, context);
    }

    @Override
    public void start()
    {
        this.lastMetadata = Objects.requireNonNull(metadata.get(), "No data section since last metadata");
        this.dataFactory = new HeadedFlatDataFactory<T>(lastMetadata.keys(), context.getDefiningPath(), Collections.singletonList(AbstractBloombergReadDriver.NULL_STRING));
        this.fieldHandlers = this.commonDataHandler.computeFieldHandlers(this.dataFactory::getRawDataAccessor);
        this.objectFactory = (ParsedFlatDataToObject<T>) this.context.createToObjectFactory(new FieldHandlerRecordType(this.section.recordType, this.fieldHandlers));
    }

    @Override
    public String getId()
    {
        return BloombergMetadataDriverDescription.ID;
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        RawFlatData raw = this.dataFactory.createRawFlatData(1, lastMetadata.line(), lastMetadata.values());
        IChecked<RawFlatData> checkedRaw = BasicChecked.newChecked(raw, null, lastMetadata.getDefects());
        Optional<IChecked<T>> parsed = this.dataFactory.createParsed(checkedRaw, this.fieldHandlers, this.objectFactory);
        this.finished = true;
        return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
    }

    @Override
    public void stop()
    {
        this.metadata.set(null);
        this.objectFactory.finished();
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }
}
