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
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.ParsedFlatDataToObject;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.*;
import java.util.stream.Stream;

public class BloombergDataReadDriver<T> extends AbstractBloombergReadDriver<T>
{
    private ParsedFlatDataToObject<? extends T> objectFactory;
    private List<FieldHandler> fieldHandlers;
    private long recordNumber = 0;
    private HeadedFlatDataFactory<T> dataFactory;

    BloombergDataReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context, "getdata");
    }

    @Override
    public String getId()
    {
        return BloombergDataDriverDescription.ID;
    }

    @Override
    public void start()
    {
        super.start();
        findStartOfFile();
        parseMetadataUntil(START_OF_FIELDS);

        List<LineReader.Line> lines = new ArrayList<>();
        untilLine(l -> END_OF_FIELDS.equals(l.getText()), lines::add);
        String[] heads = Stream.concat(Stream.of("SECURITY", "ERROR_COUNT", "FIELD_COUNT"), lines.stream().map(LineReader.Line::getText))
                .filter(t -> t.trim().length() > 0 && !t.startsWith(COMMENT_START))
                .toArray(String[]::new);

        parseMetadataUntil(START_OF_DATA);
        setupParsing();
        dataFactory = new HeadedFlatDataFactory<>(heads, helper.context.getDefiningPath(), Collections.singletonList(NULL_STRING));
        this.fieldHandlers = this.commonDataHandler.computeFieldHandlers(dataFactory::getRawDataAccessor);
        this.objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(section.getRecordType(), fieldHandlers));
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        LineReader.Line line = nextLine();
        if (line.getText().equals(END_OF_DATA))
        {
            return finish();
        }

        String[] values = line.getText().split(FIELD_SEPARATOR_REGEXP);
        List<IDefect> defects = new ArrayList<>();
        if (values.length != dataFactory.headingsSize())
        {
            defects.add(BasicDefect.newInvalidInputErrorDefect("Expected " + dataFactory.headingsSize() + " fields but got " + values.length, context.getDefiningPath()));
        }
        RawFlatData raw = dataFactory.createRawFlatData(++recordNumber, line, values);
        IChecked<RawFlatData> checkedRaw = BasicChecked.newChecked(raw, null, defects);
        Optional<IChecked<T>> parsed = dataFactory.createParsed(checkedRaw, fieldHandlers, objectFactory);
        return parsed.map(Collections::singletonList).orElseGet(Collections::emptyList);
    }
}
