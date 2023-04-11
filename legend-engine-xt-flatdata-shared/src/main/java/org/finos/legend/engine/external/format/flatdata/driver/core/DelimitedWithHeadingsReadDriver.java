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

package org.finos.legend.engine.external.format.flatdata.driver.core;

import org.finos.legend.engine.external.format.flatdata.driver.core.data.HeadedFlatDataFactory;
import org.finos.legend.engine.external.format.flatdata.driver.core.data.NoValuesRawFlatData;
import org.finos.legend.engine.external.format.flatdata.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.driver.core.util.SimpleLine;
import org.finos.legend.engine.external.format.flatdata.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.metamodel.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DelimitedWithHeadingsReadDriver<T> extends DelimitedReadDriver<T>
{
    static final String MODELLED_COUMNNS_REQIURED = "modelledColumnsMustBePresent";
    static final String ONLY_MODELLED_COLUMNS = "onlyModelledColumnsAllowed";
    static final String MATCH_COLUMNS_CASE_INSENSITIVE = "columnsHeadingsAreCaseInsensitive";

    private final FlatDataSection section;
    private final FlatDataRecordType recordType;
    private final FlatDataProcessingContext context;
    private final List<IDefect> headingDefects = new ArrayList<>();
    private HeadedFlatDataFactory<T> dataFactory;
    private long recordNumber = 0;
    private IChecked<RawFlatData> headingsLine = null;

    DelimitedWithHeadingsReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);
        this.section = section;
        this.context = context;
        this.recordType = Objects.requireNonNull(section.recordType);
    }

    @Override
    public String getId()
    {
        return DelimitedWithHeadingsDriverDescription.ID;
    }

    @Override
    public void start()
    {
        super.start();
        // First read will establish headings or establish that they are invalid
        headingsLine = readDelimitedLine().orElseGet(() -> BasicChecked.newChecked(createInvalidFlatDataDataRecord(new SimpleLine(-1, "")), null, BasicDefect.newInvalidInputCriticalDefect("Header row is missing.", context.getDefiningPath())));

        boolean headingsRequired = FlatDataUtils.getBoolean(section.sectionProperties, MODELLED_COUMNNS_REQIURED);
        boolean onlyModelled = FlatDataUtils.getBoolean(section.sectionProperties, ONLY_MODELLED_COLUMNS);

        if (headingsLine.getValue() == null || !headingsLine.getDefects().isEmpty() || dataFactory == null)
        {
            headingDefects.addAll(headingsLine.getDefects());
            headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Header row is invalid. Skipping all data in this section.", context.getDefiningPath()));
        }
        else
        {
            for (FlatDataRecordField field : recordType.fields)
            {
                if (headingsRequired && !dataFactory.containsHeading(field.label))
                {
                    headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Heading " + field.label + " missing for required column", context.getDefiningPath()));
                }
                else if (!field.type.optional && !dataFactory.containsHeading(field.label))
                {
                    headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Heading " + field.label + " missing for mandatory column", context.getDefiningPath()));
                }
            }
            if (onlyModelled)
            {
                for (String heading : dataFactory.headings())
                {
                    if (recordType.fields.stream().noneMatch(field -> heading.equals(field.label)))
                    {
                        headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Unexpected heading " + heading, context.getDefiningPath()));
                    }
                }
            }
            if (!headingDefects.isEmpty())
            {
                headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Header row is invalid. Skipping all data in this section.", context.getDefiningPath()));
            }
        }

        if (dataFactory != null)
        {
            this.fieldHandlers = this.commonDataHandler.computeFieldHandlers(dataFactory::getRawDataAccessor);
            this.objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(section.recordType, fieldHandlers));
        }
    }

    @Override
    public Collection<IChecked<T>> readCheckedObjects()
    {
        if (!headingDefects.isEmpty())
        {
            while (!isFinished())
            {
                readDelimitedLine();
            }
            return Collections.singletonList(BasicChecked.newChecked(null, headingsLine.getValue(), headingDefects));
        }

        return readDelimitedLine()
                .flatMap(raw -> dataFactory.createParsed(raw, fieldHandlers, objectFactory))
                .map(Collections::singletonList)
                .orElseGet(Collections::emptyList);
    }

    @Override
    protected RawFlatData createFlatDataDataRecord(LineReader.Line line, List<String> values)
    {
        if (dataFactory == null)
        {
            boolean caseInsensitive = FlatDataUtils.getBoolean(section.sectionProperties, MATCH_COLUMNS_CASE_INSENSITIVE);

            List<String> headings = new ArrayList<>();
            if (caseInsensitive)
            {
                for (String value : values)
                {
                    String heading = value;
                    for (FlatDataRecordField field : recordType.fields)
                    {
                        if (value.equalsIgnoreCase(field.label))
                        {
                            heading = field.label;
                        }
                    }
                    headings.add(heading);
                }
            }
            else
            {
                headings = values;
            }

            dataFactory = new HeadedFlatDataFactory<>(headings, helper.context.getDefiningPath(), helper.nullStrings);
            return dataFactory.createRawFlatData(0, line, values);
        }
        else
        {
            return dataFactory.createRawFlatData(++recordNumber, line, values);
        }
    }

    @Override
    protected RawFlatData createInvalidFlatDataDataRecord(LineReader.Line line)
    {
        long recNo = dataFactory == null ? 0 : ++recordNumber;
        return new NoValuesRawFlatData(recNo, line.getLineNumber(), line.getText());
    }
}
