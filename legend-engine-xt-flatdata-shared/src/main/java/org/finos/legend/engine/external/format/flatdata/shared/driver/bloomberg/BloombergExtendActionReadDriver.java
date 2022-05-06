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
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.ObjectVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataReadDriver;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BloombergExtendActionReadDriver<T> implements FlatDataReadDriver<T>
{
    static final String ACTION_FLAGS = "actionFlags";
    static final String MNEMONICS = "mnemonics";

    private FlatDataSection section;
    private final FlatDataProcessingContext context;
    private final ObjectVariable<FlatDataRecordType> actionsRecordType;
    private final List<String> actionFlags;
    private final List<String> mnemonics;
    private ExtendedDataFactories<T> dataFactories;

    BloombergExtendActionReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        this.section = section;
        this.context = context;
        this.actionsRecordType = ObjectVariable.reference(context, BloombergActionsReadDriver.VARIABLE_ACTIONS_RECORD);
        this.actionFlags = FlatDataUtils.getStrings(section.getSectionProperties(), ACTION_FLAGS).orElseGet(Collections::emptyList);
        this.mnemonics = FlatDataUtils.getStrings(section.getSectionProperties(), MNEMONICS).orElseGet(Collections::emptyList);
    }

    @Override
    public void start()
    {
        FlatDataRecordType actionsRecord = actionsRecordType.get();
        List<String> extendFieldNames = section.getRecordType().getFields().stream().map(FlatDataRecordField::getLabel).collect(Collectors.toList());
        FlatDataRecordType compoundRecord = new FlatDataRecordType();
        actionsRecord.getFields().stream()
                .filter(f -> !extendFieldNames.contains(f.getLabel()))
                .forEach(compoundRecord::withField);
        compoundRecord.withFields(section.getRecordType().getFields());

        CommonDataHandler commonDataHandler = new CommonDataHandler(compoundRecord, section.getSectionProperties(), context);
        List<FieldHandler> fieldHandlers = commonDataHandler.computeFieldHandlers(HeadedFlatDataFactory::getDynamicRawDataAccessor);
        ParsedFlatDataToObject<? extends T> objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(compoundRecord, fieldHandlers));
        this.dataFactories = new ExtendedDataFactories<>(context.getDefiningPath(), objectFactory, fieldHandlers);
    }

    @Override
    public String getId()
    {
        return BloombergMetadataDriverDescription.ID;
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        return Collections.emptyList();
    }

    @Override
    public void stop()
    {
        // No op
    }

    @Override
    public boolean isFinished()
    {
        throw new UnsupportedOperationException("Not used in this driver");
    }

    boolean matches(String actionFlag, String mnemonic)
    {
        return actionFlags.contains(actionFlag)
                && (mnemonics.isEmpty() || mnemonics.contains(mnemonic));
    }

    Optional<IChecked<T>> createParsed(String[] valuesIn, LineReader.Line line, long recordNumber)
    {
        return dataFactories.createParsed(valuesIn, line, recordNumber);
    }
}
