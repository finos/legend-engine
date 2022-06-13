//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.flatdata.shared.driver.core;

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.connection.CharCursor;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.LineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.SimpleLineReader;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;

import java.util.Collection;
import java.util.Collections;
import java.util.function.LongSupplier;

public class ImmaterialLinesReadDriver extends StreamingReadDriver<Object>
{
    public static final String ID = "ImmaterialLines";

    protected ImmaterialLinesReadDriver(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(new StreamingDriverHelper(section, context));
    }

    @Override
    protected LineReader createLineReader(CharCursor cursor, LongSupplier lineNumberSupplier)
    {
        return new SimpleLineReader(cursor, helper.eol, lineNumberSupplier);
    }

    @Override
    public String getId()
    {
        return ImmaterialLinesReadDriver.ID;
    }

    @Override
    public Collection<IChecked<Object>> readCheckedObjects()
    {
        untilLine(l -> isFinished(), NO_OP);
        return Collections.emptyList();
    }
}
