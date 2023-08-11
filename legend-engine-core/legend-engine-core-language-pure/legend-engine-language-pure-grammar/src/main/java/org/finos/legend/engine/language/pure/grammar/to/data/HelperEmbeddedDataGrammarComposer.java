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

package org.finos.legend.engine.language.pure.grammar.to.data;

import org.finos.legend.engine.language.pure.grammar.from.data.embedded.ExternalFormatEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.ModelStoreEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.ReferenceEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ModelStoreData;

import java.util.Objects;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperEmbeddedDataGrammarComposer
{
    public static <T extends EmbeddedData> String composeEmbeddedData(EmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        String indentedString = context.getIndentationString() + PureGrammarComposerUtility.getTabString(1);
        PureGrammarComposerContext updatedContext = PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(indentedString).build();

        ContentWithType contentWithType = context.extraEmbeddedDataComposers
                .stream()
                .map(composer -> composer.value(embeddedData, updatedContext))
                .filter(Objects::nonNull)
                .findFirst().orElseThrow(() -> new UnsupportedOperationException(unsupported(embeddedData.getClass())));

        return context.getIndentationString() + contentWithType.type + "\n"
                + context.getIndentationString() + "#{\n"
                + contentWithType.content + "\n"
                + context.getIndentationString() + "}#";
    }

    public static ContentWithType composeCoreEmbeddedDataTypes(EmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        if (embeddedData instanceof ExternalFormatData)
        {
            ExternalFormatData externalFormatData = (ExternalFormatData) embeddedData;
            String content = context.getIndentationString() + "contentType: " + PureGrammarComposerUtility.convertString(externalFormatData.contentType, true) + ";\n" +
                    context.getIndentationString() + "data: " + PureGrammarComposerUtility.convertString(externalFormatData.data, true) + ";";

            return new ContentWithType(ExternalFormatEmbeddedDataParser.TYPE, content);
        }
        else if (embeddedData instanceof DataElementReference)
        {
            String content = context.getIndentationString() + PureGrammarComposerUtility.convertPath(((DataElementReference) embeddedData).dataElement);

            return new ContentWithType(ReferenceEmbeddedDataParser.TYPE, content);
        }
        else if (embeddedData instanceof ModelStoreData)
        {
            String content = new ModelStoreDataGrammarComposer(context).compose((ModelStoreData) embeddedData);

            return new ContentWithType(ModelStoreEmbeddedDataParser.TYPE, content);
        }
        else
        {
            return null;
        }
    }
}
