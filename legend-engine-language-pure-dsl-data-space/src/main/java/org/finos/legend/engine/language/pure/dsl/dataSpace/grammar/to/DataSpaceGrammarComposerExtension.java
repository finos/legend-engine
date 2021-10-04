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

package org.finos.legend.engine.language.pure.dsl.dataSpace.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.dataSpace.grammar.from.DataSpaceParserExtension;
import org.finos.legend.engine.language.pure.grammar.to.HelperDomainGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportInfo;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class DataSpaceGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.mutable.with((elements, context, sectionName) ->
        {
            if (!DataSpaceParserExtension.NAME.equals(sectionName))
            {
                return null;
            }
            return ListIterate.collect(elements, element ->
            {
                if (element instanceof DataSpace)
                {
                    return renderDataSpace((DataSpace) element);
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
            List<DataSpace> composableElements = ListIterate.selectInstancesOf(elements, DataSpace.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, DataSpaceGrammarComposerExtension::renderDataSpace).makeString("###" + DataSpaceParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderDataSpaceSupportInfo(DataSpaceSupportInfo dataSpaceSupportInfo)
    {
        return getTabString() + "{\n" +
            (dataSpaceSupportInfo.description != null ? (getTabString(2) + "description: " + convertString(dataSpaceSupportInfo.description, true) + ";\n") : "") +
            (dataSpaceSupportInfo.contacts != null ? (getTabString(2) + "contacts:" + (dataSpaceSupportInfo.contacts.isEmpty() ? " []" : "\n" + getTabString(2) + "[\n" + getTabString(3) + ListIterate.collect(dataSpaceSupportInfo.contacts, contact -> convertString(contact, true)).makeString(",\n" + getTabString(3)) + "\n" + getTabString(2) + "]") + ";\n") : "") +
            getTabString() + "}";
    }

    private static String renderDataSpaceExecutionContext(DataSpaceExecutionContext executionContext)
    {
        return getTabString(2) + "{\n" +
            (getTabString(3) + "name: " + convertString(executionContext.name, true) + ";\n") +
            (executionContext.description != null ? (getTabString(3) + "description: " + convertString(executionContext.description, true) + ";\n") : "") +
            getTabString(3) + "mapping: " + PureGrammarComposerUtility.convertPath(executionContext.mapping) + ";\n" +
            getTabString(3) + "defaultRuntime: " + PureGrammarComposerUtility.convertPath(executionContext.defaultRuntime) + ";\n" +
            getTabString(2) + "}";
    }

    private static String renderDataSpace(DataSpace dataSpace)
    {
        return "DataSpace " + HelperDomainGrammarComposer.renderAnnotations(dataSpace.stereotypes, dataSpace.taggedValues) + PureGrammarComposerUtility.convertPath(dataSpace.getPath()) + "\n" +
            "{\n" +
            getTabString() + "groupId: " + convertString(dataSpace.groupId, true) + ";\n" +
            getTabString() + "artifactId: " + convertString(dataSpace.artifactId, true) + ";\n" +
            getTabString() + "versionId: " + convertString(dataSpace.versionId, true) + ";\n" +
            getTabString() + "executionContexts:" + (dataSpace.executionContexts.isEmpty() ? " []" : "\n" + getTabString() + "[\n" + ListIterate.collect(dataSpace.executionContexts, DataSpaceGrammarComposerExtension::renderDataSpaceExecutionContext).makeString(",\n" + getTabString(2)) + "\n" + getTabString() + "]") + ";\n" +
            getTabString() + "defaultExecutionContext: " + convertString(dataSpace.defaultExecutionContext, true) + ";\n" +
            (dataSpace.description != null ? (getTabString() + "description: " + convertString(dataSpace.description, true) + ";\n") : "") +
            (dataSpace.featuredDiagrams != null ? (getTabString() + "featuredDiagrams:" + (dataSpace.featuredDiagrams.isEmpty() ? " []" : "\n" + getTabString() + "[\n" + getTabString(2) + ListAdapter.adapt(dataSpace.featuredDiagrams).makeString(",\n" + getTabString(2)) + "\n" + getTabString() + "]") + ";\n") : "") +
            (dataSpace.supportInfo != null ? (getTabString() + "supportInfo\n" + renderDataSpaceSupportInfo(dataSpace.supportInfo) + ";\n") : "") +
            "}";
    }
}
