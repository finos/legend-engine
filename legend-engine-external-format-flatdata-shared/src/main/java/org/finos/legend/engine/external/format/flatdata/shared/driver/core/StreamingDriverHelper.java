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

import org.finos.legend.engine.external.format.flatdata.shared.driver.core.util.FlatDataUtils;
import org.finos.legend.engine.external.format.flatdata.shared.driver.core.variables.IntegerVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataVariable;
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.VariableType;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.List;

public class StreamingDriverHelper
{
    public static final String RECORD_SEPARATOR = "recordSeparator";
    public static final String MAY_CONTAIN_BLANK_LINES = "mayContainBlankLines";
    public static final String SCOPE = "scope";
    public static final String UNTIL_EOF = "untilEof";
    public static final String DEFAULT = "default";
    public static final String FOR_NUMBER_OF_LINES = "forNumberOfLines";
    public static final String UNTIL_LINE_EQUALS = "untilLineEquals";

    public static final FlatDataVariable VARIABLE_LINE_NUMBER = new FlatDataVariable("lineNumber", VariableType.Integer);

    public final FlatDataProcessingContext context;
    public final FlatDataSection section;
    public final boolean skipBlankLines;
    public final String eol;

    public StreamingDriverHelper(FlatDataSection section, FlatDataProcessingContext context)
    {
        this.section = section;
        List<FlatDataProperty> properties = section.getSectionProperties();
        this.eol = FlatDataUtils.getString(properties, RECORD_SEPARATOR).orElse(null);
        this.skipBlankLines = FlatDataUtils.getBoolean(properties, MAY_CONTAIN_BLANK_LINES);
        this.context = context;
    }

    public IntegerVariable lineNumber()
    {
        return IntegerVariable.initializeIfMissing(context, VARIABLE_LINE_NUMBER, 0);
    }
}
