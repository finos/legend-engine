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
import org.eclipse.collections.api.factory.Lists;
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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportEmail;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportInfo;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class DataSpaceGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, String, String>> getExtraSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, sectionName) ->
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
        return Lists.fixedSize.with((elements, context, composedSections) ->
        {
            List<DataSpace> composableElements = ListIterate.selectInstancesOf(elements, DataSpace.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, DataSpaceGrammarComposerExtension::renderDataSpace).makeString("###" + DataSpaceParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderDataSpaceSupportInfo(DataSpaceSupportInfo dataSpaceSupportInfo)
    {
        if (dataSpaceSupportInfo instanceof DataSpaceSupportEmail)
        {
            return "Email {\n" +
                    getTabString(2) + "address: " + convertString(((DataSpaceSupportEmail) dataSpaceSupportInfo).address, true) + ";\n" +
                    getTabString() + "}";
        }
        else
        {
            return getTabString() + "/* Unsupported data space support info type */";
        }
    }

    private static String renderDataSpaceExecutionContext(DataSpaceExecutionContext executionContext)
    {
        return getTabString(2) + "{\n" +
                (getTabString(3) + "name: " + convertString(executionContext.name, true) + ";\n") +
                (executionContext.description != null ? (getTabString(3) + "description: " + convertString(executionContext.description, true) + ";\n") : "") +
                getTabString(3) + "mapping: " + PureGrammarComposerUtility.convertPath(executionContext.mapping.path) + ";\n" +
                getTabString(3) + "defaultRuntime: " + PureGrammarComposerUtility.convertPath(executionContext.defaultRuntime.path) + ";\n" +
                getTabString(2) + "}";
    }

    private static String renderDataSpace(DataSpace dataSpace)
    {
        return "DataSpace " + HelperDomainGrammarComposer.renderAnnotations(dataSpace.stereotypes, dataSpace.taggedValues) + PureGrammarComposerUtility.convertPath(dataSpace.getPath()) + "\n" +
                "{\n" +
                getTabString() + "executionContexts:" + (dataSpace.executionContexts.isEmpty() ? " []" : "\n" + getTabString() + "[\n" + ListIterate.collect(dataSpace.executionContexts, DataSpaceGrammarComposerExtension::renderDataSpaceExecutionContext).makeString(",\n" + getTabString(2)) + "\n" + getTabString() + "]") + ";\n" +
                getTabString() + "defaultExecutionContext: " + convertString(dataSpace.defaultExecutionContext, true) + ";\n" +
                (dataSpace.description != null ? (getTabString() + "description: " + convertString(dataSpace.description, true) + ";\n") : "") +
                (dataSpace.featuredDiagrams != null ? (getTabString() + "featuredDiagrams:" + (dataSpace.featuredDiagrams.isEmpty() ? " []" : "\n" + getTabString() + "[\n" + getTabString(2) + ListIterate.collect(dataSpace.featuredDiagrams, diagram -> diagram.path).makeString(",\n" + getTabString(2)) + "\n" + getTabString() + "]") + ";\n") : "") +
                (dataSpace.supportInfo != null ? (getTabString() + "supportInfo: " + renderDataSpaceSupportInfo(dataSpace.supportInfo) + ";\n") : "") +
                "}";
    }
}
