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
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.DelimitedLine;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.RawFlatData;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.BasicChecked;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

public abstract class DelimitedReadDriver<T> extends StreamingReadDriver<T>
{
    static final String DELIMITER = "delimiter";
    static final String QUOTE_CHAR = "quoteChar";
    static final String ESCAPING_CHAR = "escapingChar";
    static final String NULL_STRING = "nullString";

    protected final DelimitedDriverHelper helper;

    DelimitedReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(new DelimitedDriverHelper(section, context));
        helper = (DelimitedDriverHelper) super.helper;
    }

    @Override
    protected LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier)
    {
        return new DelimitedLineReader(cursor, helper.eol, helper.context.getDefiningPath(), lineNumberSupplier, helper.delimiter, helper.quoteChar, helper.escapeChar);
    }

    @Override
    public void stop()
    {
        this.objectFactory.finished();
    }

    Optional<IChecked<RawFlatData>> readDelimitedLine()
    {
        DelimitedLine line = (DelimitedLine) nextLine();
        if (helper.skipBlankLines && line.isEmpty())
        {
            return Optional.empty();
        }
        else if (line.getDefects().isEmpty())
        {
            return Optional.of(BasicChecked.newChecked(createFlatDataDataRecord(line, line.getValues()), null));
        }
        else
        {
            return Optional.of(BasicChecked.newChecked(createInvalidFlatDataDataRecord(line), null, line.getDefects()));
        }
    }

    protected abstract RawFlatData createFlatDataDataRecord(LineReader.Line line, List<String> values);

    protected abstract RawFlatData createInvalidFlatDataDataRecord(LineReader.Line line);
}
