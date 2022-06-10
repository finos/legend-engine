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
import org.finos.legend.engine.external.format.flatdata.shared.driver.spi.FlatDataProcessingContext;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataProperty;
import org.finos.legend.engine.external.format.flatdata.shared.model.FlatDataSection;

import java.util.Collections;
import java.util.List;

class DelimitedDriverHelper extends StreamingDriverHelper
{
    private static final String DELIMITER = "delimiter";
    private static final String QUOTE_CHAR = "quoteChar";
    private static final String ESCAPING_CHAR = "escapingChar";
    private static final String NULL_STRING = "nullString";

    final String delimiter;
    final String quoteChar;
    final String escapeChar;
    final List<String> nullStrings;

    DelimitedDriverHelper(FlatDataSection section, FlatDataProcessingContext context)
    {
        super(section, context);

        List<FlatDataProperty> properties = section.getSectionProperties();
        this.delimiter = FlatDataUtils.getString(properties, DELIMITER).orElseThrow(() -> new IllegalStateException("Delimiter not defined"));
        this.quoteChar = FlatDataUtils.getString(properties, QUOTE_CHAR).orElse(null);
        this.escapeChar = FlatDataUtils.getString(properties, ESCAPING_CHAR).orElse(null);
        this.nullStrings = FlatDataUtils.getStrings(properties, NULL_STRING).orElse(Collections.emptyList());
    }
}
