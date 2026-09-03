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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.DataSpaceParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.DataspaceDataElementReferenceParser;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperEmbeddedDataGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.data.HelperRelationElementsDataComposer;
import org.finos.legend.engine.language.pure.grammar.to.extension.ContentWithType;
import org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.relation.RelationElement;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.*;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingInclude;

import java.util.Collections;
import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer.buildSectionComposer;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.*;

public class DataSpaceGrammarComposerExtension implements PureGrammarComposerExtension
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "DataSpace");
    }

    private MutableList<Function2<PackageableElement, PureGrammarComposerContext, String>> renderers = org.eclipse.collections.impl.factory.Lists.mutable.with((element, context) ->
    {
        if (element instanceof DataSpace)
        {
            return renderDataSpace((DataSpace) element, context);
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
        return Lists.mutable.with(buildSectionComposer(DataSpaceParserExtension.NAME, renderers));
    }

    @Override
    public List<Function3<List<PackageableElement>, PureGrammarComposerContext, List<String>, PureFreeSectionGrammarComposerResult>> getExtraFreeSectionComposers()
    {
        return Lists.fixedSize.with((elements, context, composedSections) ->
        {
            List<DataSpace> composableElements = ListIterate.selectInstancesOf(elements, DataSpace.class);
            return composableElements.isEmpty() ? null : new PureFreeSectionGrammarComposerResult(LazyIterate.collect(composableElements, el -> renderDataSpace(el, context)).makeString("###" + DataSpaceParserExtension.NAME + "\n", "\n\n", ""), composableElements);
        });
    }

    private static String renderDataSpaceSupportInfo(DataSpaceSupportInfo dataSpaceSupportInfo)
    {
        if (dataSpaceSupportInfo instanceof DataSpaceSupportEmail)
        {
            DataSpaceSupportEmail supportEmail = (DataSpaceSupportEmail) dataSpaceSupportInfo;
            return "Email {\n" +
                    (supportEmail.documentationUrl != null ? (getTabString(2) + "documentationUrl: " + convertString(supportEmail.documentationUrl, true) + ";\n") : "") +
                    getTabString(2) + "address: " + convertString(supportEmail.address, true) + ";\n" +
                    getTabString() + "}";
        }
        else if (dataSpaceSupportInfo instanceof DataSpaceSupportCombinedInfo)
        {
            DataSpaceSupportCombinedInfo combinedInfo = (DataSpaceSupportCombinedInfo) dataSpaceSupportInfo;
            return "Combined {\n" +
                    (combinedInfo.documentationUrl != null ? (getTabString(2) + "documentationUrl: " + convertString(combinedInfo.documentationUrl, true) + ";\n") : "") +
                    (combinedInfo.website != null ? (getTabString(2) + "website: " + convertString(combinedInfo.website, true) + ";\n") : "") +
                    (combinedInfo.faqUrl != null ? (getTabString(2) + "faqUrl: " + convertString(combinedInfo.faqUrl, true) + ";\n") : "") +
                    (combinedInfo.supportUrl != null ? (getTabString(2) + "supportUrl: " + convertString(combinedInfo.supportUrl, true) + ";\n") : "") +
                    (combinedInfo.emails != null ? (getTabString(2) + "emails:" + (combinedInfo.emails.isEmpty() ? " []" : "\n" + getTabString(2) + "[\n" + getTabString(3) + ListIterate.collect(combinedInfo.emails, email -> convertString(email, true)).makeString(",\n" + getTabString(3)) + "\n" + getTabString(2) + "]") + ";\n") : "") +
                    getTabString() + "}";
        }
        else if (dataSpaceSupportInfo instanceof DataSpaceSupportFullInfo)
        {
            DataSpaceSupportFullInfo full = (DataSpaceSupportFullInfo) dataSpaceSupportInfo;
            return "{\n" +
                    (full.documentation != null ? (getTabString(2) + "documentation: " + renderDataSpaceLink(full.documentation) + ";\n") : "") +
                    (full.website != null ? (getTabString(2) + "website: " + renderDataSpaceLink(full.website) + ";\n") : "") +
                    (full.faqUrl != null ? (getTabString(2) + "faqUrl: " + renderDataSpaceLink(full.faqUrl) + ";\n") : "") +
                    (full.supportUrl != null ? (getTabString(2) + "supportUrl: " + renderDataSpaceLink(full.supportUrl) + ";\n") : "") +
                    (full.emails != null ? (getTabString(2) + "emails:" + (full.emails.isEmpty() ? " []" : "\n" + getTabString(2) + "[\n" + ListIterate.collect(full.emails, DataSpaceGrammarComposerExtension::renderDataSpaceEmail).makeString(",\n") + "\n" + getTabString(2) + "]") + ";\n") : "") +
                    (full.expertise != null ? (getTabString(2) + "expertise:" + (full.expertise.isEmpty() ? " []" : "\n" + getTabString(2) + "[\n" + ListIterate.collect(full.expertise, DataSpaceGrammarComposerExtension::renderDataSpaceExpertise).makeString(",\n") + "\n" + getTabString(2) + "]") + ";\n") : "") +
                    getTabString() + "}";
        }
        return getTabString() + "/* Unsupported data space support info type */";
    }

    private static String renderDataSpaceLink(DataSpaceLink link)
    {
        return "{ " +
                (link.label != null ? ("label: " + convertString(link.label, true) + "; ") : "") +
                "url: " + convertString(link.url, true) + "; }";
    }

    private static String renderDataSpaceEmail(DataSpaceEmail email)
    {
        return getTabString(3) + "{\n" +
                getTabString(4) + "title: " + convertString(email.title, true) + ";\n" +
                getTabString(4) + "address: " + convertString(email.address, true) + ";\n" +
                getTabString(3) + "}";
    }

    private static String renderDataSpaceExpertise(DataSpaceExpertise expertise)
    {
        return getTabString(3) + "{\n" +
                (expertise.description != null ? (getTabString(4) + "description: " + convertString(expertise.description, true) + ";\n") : "") +
                (expertise.expertIds != null && !expertise.expertIds.isEmpty()
                        ? (getTabString(4) + "expertIds: [" + ListIterate.collect(expertise.expertIds, id -> convertString(id, true)).makeString(", ") + "];\n")
                        : "") +
                getTabString(3) + "}";
    }

    private static String renderDataSpaceExecutionContext(DataSpaceExecutionContext executionContext, PureGrammarComposerContext context)
    {
        return getTabString(2) + "{\n" +
                (getTabString(3) + "name: " + convertString(executionContext.name, true) + ";\n") +
                (executionContext.title != null ? (getTabString(3) + "title: " + convertString(executionContext.title, true) + ";\n") : "") +
                (executionContext.description != null ? (getTabString(3) + "description: " + convertString(executionContext.description, true) + ";\n") : "") +
                (executionContext.mapping != null ? (getTabString(3) + "mapping: " + PureGrammarComposerUtility.convertPath(executionContext.mapping.path) + ";\n") : "") +
                (executionContext.mappingProvider != null ? (getTabString(3) + "mappingProvider: " + renderMappingProvider(executionContext.mappingProvider) + ";\n") : "") +
                (executionContext.defaultRuntime != null ? (getTabString(3) + "defaultRuntime: " + PureGrammarComposerUtility.convertPath(executionContext.defaultRuntime.path) + ";\n") : "") +
                (executionContext.testData == null ? "" : (renderTestData(executionContext.testData, 3, context) + "\n")) +
                getTabString(2) + "}";
    }

    private static String renderMappingProvider(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceMappingProvider provider)
    {
        String base = PureGrammarComposerUtility.convertPath(provider.element.path);
        if (provider.keys == null || provider.keys.isEmpty())
        {
            return base;
        }
        return base + "." + String.join(",", provider.keys);
    }

    private static String renderTestData(EmbeddedData embeddedData, int baseIndentation, PureGrammarComposerContext context)
    {
        StringBuilder str = new StringBuilder();

        str.append(getTabString(baseIndentation)).append("testData").append(":\n");
        str.append(HelperEmbeddedDataGrammarComposer.composeEmbeddedData(embeddedData, PureGrammarComposerContext.Builder.newInstance(context).withIndentationString(getTabString(baseIndentation + 1)).build()));
        str.append(";");

        return str.toString();
    }

    private static String renderDataSpaceDiagram(DataSpaceDiagram diagram)
    {
        return getTabString(2) + "{\n" +
                (getTabString(3) + "title: " + convertString(diagram.title, true) + ";\n") +
                (diagram.description != null ? (getTabString(3) + "description: " + convertString(diagram.description, true) + ";\n") : "") +
                getTabString(3) + "diagram: " + PureGrammarComposerUtility.convertPath(diagram.diagram.path) + ";\n" +
                getTabString(2) + "}";
    }

    private static String renderDataSpaceExecutable(DataSpaceExecutable executable, PureGrammarComposerContext context)
    {
        if (executable instanceof DataSpacePackageableElementExecutable)
        {
            return renderDataspacePackageableElementExecutable((DataSpacePackageableElementExecutable) executable, context);
        }
        else if (executable instanceof DataSpaceTemplateExecutable)
        {
            return renderDataspaceTemplateExecutable((DataSpaceTemplateExecutable) executable, context);
        }
        throw new UnsupportedOperationException();
    }

    private static String renderDataspacePackageableElementExecutable(DataSpacePackageableElementExecutable executable, PureGrammarComposerContext context)
    {
        return getTabString(2) + "{\n" +
                (executable.id == null ? "" :  (getTabString(3) + "id: " + executable.id + ";\n")) +
                (getTabString(3) + "title: " + convertString(executable.title, true) + ";\n") +
                (executable.description != null ? (getTabString(3) + "description: " + convertString(executable.description, true) + ";\n") : "") +
                getTabString(3) + "executable: " + executable.executable.path + ";\n" +
                (executable.executionContextKey != null ?  getTabString(3) + "executionContextKey: " +  convertString(executable.executionContextKey, true) + ";\n" : "") +
                renderDataspaceExecutableSampleValues(executable.sampleValues) +
                getTabString(2) + "}";
    }

    private static String renderDataspaceTemplateExecutable(DataSpaceTemplateExecutable executable, PureGrammarComposerContext context)
    {
        return getTabString(2) + "{\n" +
                (getTabString(3) + "id: " + executable.id + ";\n") +
                (getTabString(3) + "title: " + convertString(executable.title, true) + ";\n") +
                (executable.description != null ? (getTabString(3) + "description: " + convertString(executable.description, true) + ";\n") : "") +
                getTabString(3) + "query: " + executable.query.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance(context).withIndentation(getTabSize(3)).build()) + ";\n" +
                (executable.executionContextKey != null ?  getTabString(3) + "executionContextKey: " +  convertString(executable.executionContextKey, true) + ";\n" : "") +
                renderDataspaceExecutableSampleValues(executable.sampleValues) +
                getTabString(2) + "}";
    }

    private static String renderDataspaceExecutableSampleValues(RelationElement sampleValues)
    {
        if (sampleValues == null)
        {
            return "";
        }
        return getTabString(3) + "sampleValues: Relation\n" + HelperRelationElementsDataComposer.renderAlignedRelationElement(sampleValues, getTabString(4), true) + ";\n";
    }

    private static String renderDataSpace(DataSpace dataSpace, PureGrammarComposerContext context)
    {
        if (dataSpace.featuredDiagrams != null)
        {
            List<DataSpaceDiagram> featuredDiagrams = ListIterate.collect(dataSpace.featuredDiagrams, featuredDiagram ->
            {
                DataSpaceDiagram diagram = new DataSpaceDiagram();
                diagram.title = "";
                diagram.diagram = featuredDiagram;
                return diagram;
            });
            if (dataSpace.diagrams != null)
            {
                dataSpace.diagrams.addAll(featuredDiagrams);
            }
            else
            {
                dataSpace.diagrams = featuredDiagrams;
            }
        }
        return HelperDomainGrammarComposer.renderDeclarationPrefix("DataSpace", dataSpace.stereotypes, dataSpace.taggedValues) + PureGrammarComposerUtility.convertPath(dataSpace.getPath()) + "\n" +
                "{\n" +
                (dataSpace.executionContexts == null || dataSpace.executionContexts.isEmpty() ? "" : getTabString() + "executionContexts:" + "\n" + getTabString() + "[\n" + ListIterate.collect(dataSpace.executionContexts, executionContext -> DataSpaceGrammarComposerExtension.renderDataSpaceExecutionContext(executionContext, context)).makeString(",\n") + "\n" + getTabString() + "]" + ";\n") +
                (dataSpace.defaultExecutionContext != null ? getTabString() + "defaultExecutionContext: " + convertString(dataSpace.defaultExecutionContext, true) + ";\n" : "") +
                (dataSpace.title != null ? (getTabString() + "title: " + convertString(dataSpace.title, true) + ";\n") : "") +
                (dataSpace.description != null ? (getTabString() + "description: " + convertString(dataSpace.description, true) + ";\n") : "") +
                (dataSpace.diagrams != null ? (getTabString() + "diagrams:" + (dataSpace.diagrams.isEmpty() ? " []" : "\n" + getTabString() + "[\n" + ListIterate.collect(dataSpace.diagrams, DataSpaceGrammarComposerExtension::renderDataSpaceDiagram).makeString(",\n") + "\n" + getTabString() + "]") + ";\n") : "") +
                (dataSpace.elements != null ? (getTabString() + "elements:" + (dataSpace.elements.isEmpty() ? " []" : "\n" + getTabString() + "[\n" + getTabString(2) + ListIterate.collect(dataSpace.elements, element -> (element.exclude != null && element.exclude ? "-" : "") + element.path).makeString(",\n" + getTabString(2)) + "\n" + getTabString() + "]") + ";\n") : "") +
                (dataSpace.executables != null ? (getTabString() + "executables:" + (dataSpace.executables.isEmpty() ? " []" : "\n" + getTabString() + "[\n" + ListIterate.collect(dataSpace.executables, executable -> DataSpaceGrammarComposerExtension.renderDataSpaceExecutable(executable, context)).makeString(",\n") + "\n" + getTabString() + "]") + ";\n") : "") +
                (dataSpace.supportInfo != null ? (getTabString() + "supportInfo: " + renderDataSpaceSupportInfo(dataSpace.supportInfo) + ";\n") : "") +
                (dataSpace.operationalMetadata != null ? (getTabString() + "operationalMetadata: " + renderDataSpaceOperationalMetadata(dataSpace.operationalMetadata) + ";\n") : "") +
                "}";
    }

    private static String renderDataSpaceOperationalMetadata(DataSpaceOperationalMetadata operationalMetadata)
    {
        StringBuilder builder = new StringBuilder("{\n");
        if (operationalMetadata.coverageRegions != null && !operationalMetadata.coverageRegions.isEmpty())
        {
            builder.append(getTabString(2)).append("coverageRegions: [")
                    .append(ListIterate.collect(operationalMetadata.coverageRegions, Enum::name).makeString(", "))
                    .append("];\n");
        }
        if (operationalMetadata.updateFrequency != null)
        {
            builder.append(getTabString(2)).append("updateFrequency: ").append(operationalMetadata.updateFrequency.name()).append(";\n");
        }
        builder.append(getTabString()).append("}");
        return builder.toString();
    }

    @Override
    public List<Function<MappingInclude, String>> getExtraMappingIncludeComposers()
    {
        return Collections.singletonList(this::renderMappingInclude);
    }

    private String renderMappingInclude(MappingInclude mappingInclude)
    {
        if (mappingInclude.getClass() == MappingIncludeDataSpace.class)
        {
            MappingIncludeDataSpace mappingIncludeDataSpace = (MappingIncludeDataSpace) mappingInclude;
            return "include dataspace " + mappingIncludeDataSpace.includedDataSpace;
        }
        return null;
    }

    @Override
    public List<Function2<EmbeddedData, PureGrammarComposerContext, ContentWithType>> getExtraEmbeddedDataComposers()
    {
        return Collections.singletonList(this::composeDataspaceDataElementReference);
    }

    private ContentWithType composeDataspaceDataElementReference(EmbeddedData embeddedData, PureGrammarComposerContext context)
    {
        if (embeddedData instanceof DataElementReference
                && ((DataElementReference) embeddedData).dataElement.type.equals(PackageableElementType.DATASPACE))
        {
            String content = context.getIndentationString() + PureGrammarComposerUtility.convertPath(((DataElementReference) embeddedData).dataElement.path);
            return new ContentWithType(DataspaceDataElementReferenceParser.TYPE, content);
        }
        return null;
    }
}
