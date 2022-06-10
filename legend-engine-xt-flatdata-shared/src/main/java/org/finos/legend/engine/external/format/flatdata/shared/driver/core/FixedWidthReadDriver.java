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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.AddressedFlatDataFactory;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLine;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.LongSupplier;

public class FixedWidthReadDriver<T> extends StreamingReadDriver<T>
{
    private final FlatDataProcessingContext context;
    private final String[] addresses;
    private final int[] starts;
    private final int[] ends;

    private AddressedFlatDataFactory<T> dataFactory;
    private long recordNumber = 0;

    FixedWidthReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(new StreamingDriverHelper(section, context));
        this.context = context;

        this.addresses = new String[section.getRecordType().getFields().size()];
        this.starts = new int[addresses.length];
        this.ends = new int[addresses.length];
        for (int i = 0; i < addresses.length; i++)
        {
            String address = section.getRecordType().getFields().get(i).getAddress();
            int start = Integer.parseInt(address.split(":")[0]) - 1;
            int end = Integer.parseInt(address.split(":")[1]);

            this.addresses[i] = address;
            this.starts[i] = start;
            this.ends[i] = end;
        }
    }

    @Override
    public String getId()
    {
        return FixedWidthDriverDescription.ID;
    }

    @Override
    public void start()
    {
        super.start();
        this.dataFactory = new AddressedFlatDataFactory<>(addresses, context.getDefiningPath(), Collections.emptyList());
        this.fieldHandlers = this.commonDataHandler.computeFieldHandlers(dataFactory::getRawDataAccessor);
        this.objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(helper.section.getRecordType(), fieldHandlers));
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        return readLine()
                .flatMap(raw -> dataFactory.createParsed(raw, fieldHandlers, objectFactory))
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    private Optional<IChecked<RawFlatData>> readLine()
    {
        SimpleLine line = (SimpleLine) nextLine();

        if (helper.skipBlankLines && line.isEmpty())
        {
            return Optional.empty();
        }
        else
        {
            String[] values = new String[addresses.length];
            String text = line.getText();
            for (int i = 0; i < addresses.length; i++)
            {
                values[i] = ends[i] <= text.length()
                    ?  text.substring(starts[i], ends[i]).trim()
                    : "";
            }
            RawFlatData rawFlatData = dataFactory.createRawFlatData(++recordNumber, line, values);
            return Optional.of(BasicChecked.newChecked(rawFlatData, null));
        }
    }


    @Override
    protected LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier)
    {
        return new SimpleLineReader(cursor, helper.eol, lineNumberSupplier);
    }
}
