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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperEmbeddedDataGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.language.pure.grammar.to.test.assertion.HelperTestAssertionGrammarComposer;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class CorePureGrammarComposer implements PureGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if ("Data".equals(sectionName))
            {
                return ListIterate.collect(elements, element ->
                {
                    if (element instanceof DataElement)
                    {
                        return renderDataElement((DataElement) element, context);
                    }
                    return "/* Can't transform element '" + element.getPath() + "' in this section */";
                }).makeString("\n\n");
            }
            else
            {
                return null;
            }
        });
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.mutable.with((elements, context, composedSections) ->
        {
            List<DataElement> composableElements = ListIterate.selectInstancesOf(elements, DataElement.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, e -> renderDataElement(e, context)).makeString("###Data" + "\n", "\n\n", ""), composableElements);
        });
    }

    @Override
    public List<Function2<EmbeddedData, PureGrammarComposerContext, ContentWithType>> getExtraEmbeddedDataComposers()
    {
        return Lists.mutable.with(HelperEmbeddedDataGrammarComposer::composeCoreEmbeddedDataTypes);
    }

    @Override
    public List<Function2<TestAssertion, PureGrammarComposerContext, ContentWithType>> getExtraTestAssertionComposers()
    {
        return Lists.mutable.with(HelperTestAssertionGrammarComposer::composeCoreTestAssertion);
    }

    public static String renderDataElement(DataElement dataElement, PureGrammarComposerContext context)
    {
        PureGrammarComposerContext updatedContext = PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(PureGrammarComposerUtility.getTabString()).build();
        StringBuilder str = new StringBuilder();

        str.append("Data ")
                .append(HelperDomainGrammarComposer.renderAnnotations(dataElement.stereotypes, dataElement.taggedValues))
                .append(PureGrammarComposerUtility.convertPath(dataElement.getPath()))
                .append("\n");

        str.append("{\n");
        if (dataElement.documentation != null)
        {
            str.append(getTabString()).append("documentation: ").append(convertString(dataElement.documentation, true)).append(";\n");
        }
        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(dataElement.data, updatedContext));
        str.append("\n}");

        return str.toString();
    }
}
