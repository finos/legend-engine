// Copyright 2020 Goldman Sachs
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

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.TextParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.text.Text;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class TextGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if (!TextParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof Text)
                {
                    return renderText((Text) element);
                }
                return "/* Can't transform element '" + element.getPath() + "' in this section */";
            }).makeString("\n\n");
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.mutable.with((elements, context, composedSections) ->
        {
            List<Text> composableElements = ListIterate.selectInstancesOf(elements, Text.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, TextGrammarComposerExtension::renderText).makeString("###" + TextParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderText(Text text)
    {
        return "Text " + PureGrammarComposerUtility.convertPath(text.getPath()) + "\n" +
            "{\n" +
            (text.type != null ? (getTabString() + "type: " + text.type + ";\n") : "") +
            getTabString() + "content: " + convertString(text.content, true) + ";\n" +
            "}";
    }
}
