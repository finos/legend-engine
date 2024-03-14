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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.api.list.MutableList;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingIncludeMapping;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;

public class CorePureGrammarComposer implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Core");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = Lists.mutable.with((element, context) ->
    {
        if (element instanceof DataElement)
        {
            return renderDataElement((DataElement) element, context);
        }
        return null;
    });

    @Override
    public MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> getExtraPackageableElementComposers()
    {
        return renderers;
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with(buildSectionComposer("Data", renderers));
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
        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(dataElement.data, updatedContext));
        str.append("\n}");

        return str.toString();
    }

    @Override
    public List<Function<MappingInclude, String>> getExtraMappingIncludeComposers()
    {
        return Collections.singletonList(this::renderMappingInclude);
    }

    private <T extends MappingInclude> String renderMappingInclude(T mappingInclude)
    {
        if (mappingInclude.getClass() == MappingIncludeMapping.class)
        {
            MappingIncludeMapping mappingIncludeMapping = (MappingIncludeMapping) mappingInclude;
            return "include mapping " + mappingIncludeMapping.getIncludedMapping()
                    + (mappingIncludeMapping.sourceDatabasePath != null && mappingIncludeMapping.targetDatabasePath != null ? "[" + PureGrammarComposerUtility.convertPath(mappingIncludeMapping.sourceDatabasePath) + "->" + PureGrammarComposerUtility.convertPath(mappingIncludeMapping.targetDatabasePath) + "]" : "");
        }
        return null;
    }
}
