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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.HeadedFlatDataFactory;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.CommonDataHandler;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ObjectVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.*;

public class BloombergMetadataReadDriver<T> implements FlatDataReadDriver<T>
{
    private FlatDataSection section;
    private final ObjectVariable<BloombergKeyValues> metadata;
    private final FlatDataProcessingContext context;
    private final CommonDataHandler commonDataHandler;
    private final FlatDataRecordType recordType;
    private boolean finished = false;

    BloombergMetadataReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        this.recordType = Objects.requireNonNull(section.getRecordType());
        this.section = section;
        this.metadata = ObjectVariable.reference(context, BloombergKeyValues.VARIABLE_LAST_METADATA);
        this.context = context;
        this.commonDataHandler = new CommonDataHandler(section, context);
    }

    @Override
    public void start()
    {
        // No op
    }

    @Override
    public String getId()
    {
        return BloombergMetadataDriverDescription.ID;
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        BloombergKeyValues lastMetadata = Objects.requireNonNull(metadata.get(), "No data section since last metadata");
        HeadedFlatDataFactory<T> dataFactory = new HeadedFlatDataFactory<T>(lastMetadata.keys(), context.getDefiningPath(), Collections.singletonList(AbstractBloombergReadDriver.NULL_STRING));
        List<FieldHandler> fieldHandlers = commonDataHandler.computeFieldHandlers(dataFactory::getRawDataAccessor);
        ParsedFlatDataToObject<T> objectFactory = (ParsedFlatDataToObject<T>) context.createToObjectFactory(new FieldHandlerRecordType(section.getRecordType(), fieldHandlers));
        RawFlatData raw = dataFactory.createRawFlatData(1, lastMetadata.line(), lastMetadata.values());
        IChecked<RawFlatData> checkedRaw = BasicChecked.newChecked(raw, null, lastMetadata.getDefects());
        Optional<IChecked<T>> parsed = dataFactory.createParsed(checkedRaw, fieldHandlers, objectFactory);
        finished = true;
        metadata.set(null);
        return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
    }

    @Override
    public void stop()
    {
        // No op
    }

    @Override
    public boolean isFinished()
    {
        return finished;
    }
}
