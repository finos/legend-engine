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

package org.finos.legend.engine.language.pure.grammar.to;

import org.finos.legend.engine.protocol.pure.v1.model.data.*;

import java.util.Objects;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.unsupported;

public class HelperEmbeddedDataGrammarComposer
{

    public static <T extends EmbeddedData> String composeEmbeddedData(EmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        String text = context.extraEmbeddedDataComposers.stream().map(composer -> composer.value(embeddedData, context)).filter(Objects::nonNull).findFirst().orElseGet(() -> unsupported(embeddedData.getClass()));
        return text.contains("\n")
                ? embeddedData._type + " #{\n" + context.getIndentationString() + PureGrammarComposerUtility.getTabString(1) + text + "\n}#"
                : embeddedData._type + " #{ " + text + " }#";
    }

    public static String composeCoreEmbeddedDataTypes(EmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        if (embeddedData instanceof BinaryData)
        {
            BinaryData binaryData = (BinaryData) embeddedData;
            return "contentType: " + PureGrammarComposerUtility.convertString(binaryData.contentType, true) + ";\n" +
                    context.getIndentationString() + PureGrammarComposerUtility.getTabString() + "data: " + PureGrammarComposerUtility.convertString(formatHex(binaryData.hexData), true) + ";";
        }
        else if (embeddedData instanceof DataElementReference)
        {
            return PureGrammarComposerUtility.convertPath(((DataElementReference) embeddedData).dataElement);
        }
        else if (embeddedData instanceof PureCollectionData)
        {
            return  new PureCollectionDataGrammarComposer(context).compose((PureCollectionData) embeddedData);
        }
        else if (embeddedData instanceof TextData)
        {
            TextData textData = (TextData) embeddedData;
            return "contentType: " + PureGrammarComposerUtility.convertString(textData.contentType, true) + ";\n" +
                    context.getIndentationString() + PureGrammarComposerUtility.getTabString() + "data: " + PureGrammarComposerUtility.convertString(textData.data, true) + ";";
        }
        else
        {
            return null;
        }
    }

    private static String formatHex(String s)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
        {
            if (i > 0 && i % 4 == 0)
            {
                builder.append(" ");
            }
            builder.append(s.charAt(i));
        }
        return builder.toString();
    }
}
