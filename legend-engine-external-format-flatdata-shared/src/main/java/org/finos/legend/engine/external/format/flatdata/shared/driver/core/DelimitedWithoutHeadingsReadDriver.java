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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.NoValuesRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.PositionalFlatDataFactory;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DelimitedWithoutHeadingsReadDriver<T> extends DelimitedReadDriver<T>
{
    public static final String ID = "DelimitedWithoutHeadings";
    private final FlatDataProcessingContext context;

    private PositionalFlatDataFactory<T> dataFactory;
    private long recordNumber = 0;

    DelimitedWithoutHeadingsReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);
        this.context = context;
    }

    @Override
    public void start()
    {
        super.start();
        this.dataFactory = new PositionalFlatDataFactory<>(context.getDefiningPath(), helper.nullStrings);
        this.fieldHandlers = this.commonDataHandler.computeFieldHandlers(dataFactory::getRawDataAccessor);
        this.objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(helper.section.getRecordType(), fieldHandlers));
    }

    @Override
    public String getId()
    {
        return DelimitedWithoutHeadingsReadDriver.ID;
    }


    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        return readDelimitedLine()
                .flatMap(raw -> dataFactory.createParsed(raw, fieldHandlers, objectFactory))
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    @Override
    protected RawFlatData createFlatDataDataRecord(LineReader.Line line, List<String> values)
    {
        return dataFactory.createRawFlatData(++recordNumber, line, values);
    }

    @Override
    protected RawFlatData createInvalidFlatDataDataRecord(LineReader.Line line)
    {
        return new NoValuesRawFlatData(++recordNumber, line.getLineNumber(), line.getText());
    }
}
