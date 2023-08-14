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

package org.finos.legend.engine.language.pure.grammar.from.data.embedded;

import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.data.EmbeddedDataParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class ReferenceEmbeddedDataParser implements EmbeddedDataParser
{
    public static final String TYPE = "Reference";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public EmbeddedData parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        if ("".equals(code.trim()))
        {
            throw new EngineException("Path should be provided for DataElementReference", sourceInformation, EngineErrorType.PARSER);
        }
        String validPath = PureGrammarParserUtility.validatePath(code.trim(), sourceInformation);

        DataElementReference result = new DataElementReference();
        result.sourceInformation = sourceInformation;
        result.dataElement = validPath;
        return result;
    }
}
