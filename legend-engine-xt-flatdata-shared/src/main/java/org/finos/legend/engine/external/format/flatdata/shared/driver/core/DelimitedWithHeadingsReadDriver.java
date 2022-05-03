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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.HeadedFlatDataFactory;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.data.NoValuesRawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.fieldHandler.FieldHandlerRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLine;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordField;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataRecordType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicDefect;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IDefect;

import java.util.*;

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
        this.recordType = Objects.requireNonNull(section.getRecordType());
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

        boolean headingsRequired = FlatDataUtils.getBoolean(section.getSectionProperties(), MODELLED_COUMNNS_REQIURED);
        boolean onlyModelled = FlatDataUtils.getBoolean(section.getSectionProperties(), ONLY_MODELLED_COLUMNS);

        if (headingsLine.getValue() == null || !headingsLine.getDefects().isEmpty() || dataFactory == null)
        {
            headingDefects.addAll(headingsLine.getDefects());
            headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Header row is invalid. Skipping all data in this section.", context.getDefiningPath()));
        }
        else
        {
            for (FlatDataRecordField field : recordType.getFields())
            {
                if (headingsRequired && !dataFactory.containsHeading(field.getLabel()))
                {
                    headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Heading " + field.getLabel() + " missing for required column", context.getDefiningPath()));
                }
                else if (!field.isOptional() && !dataFactory.containsHeading(field.getLabel()))
                {
                    headingDefects.add(BasicDefect.newInvalidInputCriticalDefect("Heading " + field.getLabel() + " missing for mandatory column", context.getDefiningPath()));
                }
            }
            if (onlyModelled)
            {
                for (String heading : dataFactory.headings())
                {
                    if (recordType.getFields().stream().noneMatch(field -> heading.equals(field.getLabel())))
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
            this.objectFactory = context.createToObjectFactory(new FieldHandlerRecordType(section.getRecordType(), fieldHandlers));
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
            boolean caseInsensitive = FlatDataUtils.getBoolean(section.getSectionProperties(), MATCH_COLUMNS_CASE_INSENSITIVE);

            List<String> headings = new ArrayList<>();
            if (caseInsensitive)
            {
                for (String value : values)
                {
                    String heading = value;
                    for (FlatDataRecordField field : recordType.getFields())
                    {
                        if (value.equalsIgnoreCase(field.getLabel()))
                        {
                            heading = field.getLabel();
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
