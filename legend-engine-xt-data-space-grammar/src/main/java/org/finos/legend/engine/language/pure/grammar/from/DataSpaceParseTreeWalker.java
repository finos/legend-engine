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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceDiagram;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceExecutable;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportCombinedInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportEmail;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceSupportInfo;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class DataSpaceParseTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public DataSpaceParseTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(DataSpaceParserGrammar.DefinitionContext ctx)
    {
        ctx.dataSpaceElement().stream().map(this::visitDataSpace).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
    }

    private DataSpace visitDataSpace(DataSpaceParserGrammar.DataSpaceElementContext ctx)
    {
        DataSpace dataSpace = new DataSpace();
        dataSpace.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        dataSpace._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        dataSpace.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        dataSpace.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        dataSpace.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        // Execution contexts
        DataSpaceParserGrammar.ExecutionContextsContext executionContextsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContexts(), "executionContexts", dataSpace.sourceInformation);
        dataSpace.executionContexts = ListIterate.collect(executionContextsContext.executionContext(), executionContext -> this.visitDataSpaceExecutionContext(executionContext));

        // Default execution context
        DataSpaceParserGrammar.DefaultExecutionContextContext defaultExecutionContextContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.defaultExecutionContext(), "defaultExecutionContext", dataSpace.sourceInformation);
        dataSpace.defaultExecutionContext = PureGrammarParserUtility.fromGrammarString(defaultExecutionContextContext.STRING().getText(), true);

        // Title (optional)
        DataSpaceParserGrammar.TitleContext titleContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.title(), "title", dataSpace.sourceInformation);
        dataSpace.title = titleContext != null ? PureGrammarParserUtility.fromGrammarString(titleContext.STRING().getText(), true) : null;

        // Description (optional)
        DataSpaceParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", dataSpace.sourceInformation);
        dataSpace.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Elements (optional)
        DataSpaceParserGrammar.ElementsContext elementsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.elements(), "elements", dataSpace.sourceInformation);
        dataSpace.elements = elementsContext != null ? ListIterate.collect(elementsContext.elementScopePath(), elementScopePathContext ->
        {
            DataSpaceElementPointer pointer = new DataSpaceElementPointer();
            pointer.path = PureGrammarParserUtility.fromQualifiedName(elementScopePathContext.qualifiedName().packagePath() == null ? Collections.emptyList() : elementScopePathContext.qualifiedName().packagePath().identifier(), elementScopePathContext.qualifiedName().identifier());
            pointer.sourceInformation = walkerSourceInformation.getSourceInformation(elementScopePathContext);
            if (elementScopePathContext.MINUS() != null)
            {
                pointer.exclude = true;
            }
            return pointer;
        }) : null;

        // Executables (optional)
        DataSpaceParserGrammar.ExecutablesContext executablesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executables(), "executables", dataSpace.sourceInformation);
        dataSpace.executables = executablesContext != null ? ListIterate.collect(executablesContext.executable(), this::visitDataSpaceExecutable) : null;

        // Diagrams (optional)
        DataSpaceParserGrammar.DiagramsContext diagramsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.diagrams(), "diagrams", dataSpace.sourceInformation);
        dataSpace.diagrams = diagramsContext != null ? ListIterate.collect(diagramsContext.diagram(), this::visitDataSpaceDiagram) : null;

        // Featured diagrams (optional)
        // This has been deprecated in favor of diagrams
        DataSpaceParserGrammar.FeaturedDiagramsContext featuredDiagramsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.featuredDiagrams(), "featuredDiagrams", dataSpace.sourceInformation);
        if (featuredDiagramsContext != null)
        {
            List<DataSpaceDiagram> featuredDiagrams = ListIterate.collect(featuredDiagramsContext.qualifiedName(), diagramPathContext ->
            {
                DataSpaceDiagram diagram = new DataSpaceDiagram();
                diagram.sourceInformation = this.walkerSourceInformation.getSourceInformation(diagramPathContext);
                diagram.title = "";
                diagram.diagram = new PackageableElementPointer(
                        PureGrammarParserUtility.fromQualifiedName(diagramPathContext.packagePath() == null ? Collections.emptyList() : diagramPathContext.packagePath().identifier(), diagramPathContext.identifier())
                );
                diagram.diagram.sourceInformation = diagram.sourceInformation;
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

        // Support info (optional)
        DataSpaceParserGrammar.SupportInfoContext supportInfoContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.supportInfo(), "supportInfo", dataSpace.sourceInformation);
        dataSpace.supportInfo = supportInfoContext != null ? this.visitDataSpaceSupportInfo(supportInfoContext) : null;

        return dataSpace;
    }

    private DataSpaceExecutionContext visitDataSpaceExecutionContext(DataSpaceParserGrammar.ExecutionContextContext ctx)
    {
        DataSpaceExecutionContext executionContext = new DataSpaceExecutionContext();
        executionContext.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // Name
        DataSpaceParserGrammar.ExecutionContextNameContext executionContextNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContextName(), "name", executionContext.sourceInformation);
        executionContext.name = PureGrammarParserUtility.fromGrammarString(executionContextNameContext.STRING().getText(), true);

        // Title (optional)
        DataSpaceParserGrammar.ExecutionContextTitleContext titleContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executionContextTitle(), "title", executionContext.sourceInformation);
        executionContext.title = titleContext != null ? PureGrammarParserUtility.fromGrammarString(titleContext.STRING().getText(), true) : null;

        // Description (optional)
        DataSpaceParserGrammar.ExecutionContextDescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executionContextDescription(), "description", executionContext.sourceInformation);
        executionContext.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Mapping
        DataSpaceParserGrammar.ExecutionContextMappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContextMapping(), "mapping", executionContext.sourceInformation);
        executionContext.mapping = new PackageableElementPointer(
                PackageableElementType.MAPPING,
                PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier())
        );
        executionContext.mapping.sourceInformation = walkerSourceInformation.getSourceInformation(mappingContext);

        // Runtime
        DataSpaceParserGrammar.ExecutionContextDefaultRuntimeContext defaultRuntimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContextDefaultRuntime(), "defaultRuntime", executionContext.sourceInformation);
        executionContext.defaultRuntime = new PackageableElementPointer(
                PackageableElementType.RUNTIME,
                PureGrammarParserUtility.fromQualifiedName(defaultRuntimeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : defaultRuntimeContext.qualifiedName().packagePath().identifier(), defaultRuntimeContext.qualifiedName().identifier())
        );
        executionContext.defaultRuntime.sourceInformation = walkerSourceInformation.getSourceInformation(defaultRuntimeContext);

        return executionContext;
    }

    private DataSpaceExecutable visitDataSpaceExecutable(DataSpaceParserGrammar.ExecutableContext ctx)
    {
        DataSpaceExecutable executable = new DataSpaceExecutable();
        executable.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // Name
        DataSpaceParserGrammar.ExecutableTitleContext executableTitleContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executableTitle(), "title", executable.sourceInformation);
        executable.title = PureGrammarParserUtility.fromGrammarString(executableTitleContext.STRING().getText(), true);

        // Description (optional)
        DataSpaceParserGrammar.ExecutableDescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.executableDescription(), "description", executable.sourceInformation);
        executable.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Path
        DataSpaceParserGrammar.ExecutablePathContext pathContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executablePath(), "executable", executable.sourceInformation);
        executable.executable = new PackageableElementPointer(
                PureGrammarParserUtility.fromQualifiedName(pathContext.qualifiedName().packagePath() == null ? Collections.emptyList() : pathContext.qualifiedName().packagePath().identifier(), pathContext.qualifiedName().identifier())
        );
        executable.executable.sourceInformation = walkerSourceInformation.getSourceInformation(pathContext);

        return executable;
    }

    private DataSpaceDiagram visitDataSpaceDiagram(DataSpaceParserGrammar.DiagramContext ctx)
    {
        DataSpaceDiagram diagram = new DataSpaceDiagram();
        diagram.sourceInformation = this.walkerSourceInformation.getSourceInformation(ctx);

        // Name
        DataSpaceParserGrammar.DiagramTitleContext executableTitleContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.diagramTitle(), "title", diagram.sourceInformation);
        diagram.title = PureGrammarParserUtility.fromGrammarString(executableTitleContext.STRING().getText(), true);

        // Description (optional)
        DataSpaceParserGrammar.DiagramDescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.diagramDescription(), "description", diagram.sourceInformation);
        diagram.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Path
        DataSpaceParserGrammar.DiagramPathContext pathContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.diagramPath(), "diagram", diagram.sourceInformation);
        diagram.diagram = new PackageableElementPointer(
                PureGrammarParserUtility.fromQualifiedName(pathContext.qualifiedName().packagePath() == null ? Collections.emptyList() : pathContext.qualifiedName().packagePath().identifier(), pathContext.qualifiedName().identifier())
        );
        diagram.diagram.sourceInformation = walkerSourceInformation.getSourceInformation(pathContext);

        return diagram;
    }

    private DataSpaceSupportInfo visitDataSpaceSupportInfo(DataSpaceParserGrammar.SupportInfoContext ctx)
    {
        if (ctx.supportCombinedInfo() != null)
        {
            DataSpaceParserGrammar.SupportCombinedInfoContext combinedInfoContext = ctx.supportCombinedInfo();
            DataSpaceSupportCombinedInfo supportInfo = new DataSpaceSupportCombinedInfo();
            supportInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(combinedInfoContext);

            // Documentation URL (optional)
            DataSpaceParserGrammar.SupportDocumentationUrlContext documentationUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.supportDocumentationUrl(), "documentationUrl", supportInfo.sourceInformation);
            supportInfo.documentationUrl = documentationUrlContext != null ? PureGrammarParserUtility.fromGrammarString(documentationUrlContext.STRING().getText(), true) : null;

            // Emails (optional)
            DataSpaceParserGrammar.CombinedInfoEmailsContext emailsContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoEmails(), "emails", supportInfo.sourceInformation);
            supportInfo.emails = emailsContext != null ? ListIterate.collect(emailsContext.STRING(), val -> PureGrammarParserUtility.fromGrammarString(val.getText(), true)) : null;

            // Website (optional)
            DataSpaceParserGrammar.CombinedInfoWebsiteContext websiteContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoWebsite(), "website", supportInfo.sourceInformation);
            supportInfo.website = websiteContext != null ? PureGrammarParserUtility.fromGrammarString(websiteContext.STRING().getText(), true) : null;

            // FAQ URL (optional)
            DataSpaceParserGrammar.CombinedInfoFaqUrlContext faqUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoFaqUrl(), "faqUrl", supportInfo.sourceInformation);
            supportInfo.faqUrl = faqUrlContext != null ? PureGrammarParserUtility.fromGrammarString(faqUrlContext.STRING().getText(), true) : null;

            // Support URL (optional)
            DataSpaceParserGrammar.CombinedInfoSupportUrlContext supportUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(combinedInfoContext.combinedInfoSupportUrl(), "supportUrl", supportInfo.sourceInformation);
            supportInfo.supportUrl = supportUrlContext != null ? PureGrammarParserUtility.fromGrammarString(supportUrlContext.STRING().getText(), true) : null;

            return supportInfo;

        }
        else if (ctx.supportEmail() != null)
        {
            DataSpaceParserGrammar.SupportEmailContext supportEmailContext = ctx.supportEmail();
            DataSpaceSupportEmail supportInfo = new DataSpaceSupportEmail();
            supportInfo.sourceInformation = this.walkerSourceInformation.getSourceInformation(supportEmailContext);

            // Documentation URL (optional)
            DataSpaceParserGrammar.SupportDocumentationUrlContext documentationUrlContext = PureGrammarParserUtility.validateAndExtractOptionalField(supportEmailContext.supportDocumentationUrl(), "documentationUrl", supportInfo.sourceInformation);
            supportInfo.documentationUrl = documentationUrlContext != null ? PureGrammarParserUtility.fromGrammarString(documentationUrlContext.STRING().getText(), true) : null;

            // Address
            DataSpaceParserGrammar.SupportEmailAddressContext supportEmailAddressContext = PureGrammarParserUtility.validateAndExtractRequiredField(supportEmailContext.supportEmailAddress(), "address", supportInfo.sourceInformation);
            supportInfo.address = PureGrammarParserUtility.fromGrammarString(supportEmailAddressContext.STRING().getText(), true);

            return supportInfo;
        }
        throw new UnsupportedOperationException("Can't parse unsupported support info");
    }

    private List<TaggedValue> visitTaggedValues(DataSpaceParserGrammar.TaggedValuesContext ctx)
    {
        return ListIterate.collect(ctx.taggedValue(), taggedValueContext ->
        {
            TaggedValue taggedValue = new TaggedValue();
            TagPtr tagPtr = new TagPtr();
            taggedValue.tag = tagPtr;
            tagPtr.profile = PureGrammarParserUtility.fromQualifiedName(taggedValueContext.qualifiedName().packagePath() == null ? Collections.emptyList() : taggedValueContext.qualifiedName().packagePath().identifier(), taggedValueContext.qualifiedName().identifier());
            tagPtr.value = PureGrammarParserUtility.fromIdentifier(taggedValueContext.identifier());
            taggedValue.value = PureGrammarParserUtility.fromGrammarString(taggedValueContext.STRING().getText(), true);
            taggedValue.tag.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.qualifiedName());
            taggedValue.tag.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext.identifier());
            taggedValue.sourceInformation = this.walkerSourceInformation.getSourceInformation(taggedValueContext);
            return taggedValue;
        });
    }

    private List<StereotypePtr> visitStereotypes(DataSpaceParserGrammar.StereotypesContext ctx)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext ->
        {
            StereotypePtr stereotypePtr = new StereotypePtr();
            stereotypePtr.profile = PureGrammarParserUtility.fromQualifiedName(stereotypeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : stereotypeContext.qualifiedName().packagePath().identifier(), stereotypeContext.qualifiedName().identifier());
            stereotypePtr.value = PureGrammarParserUtility.fromIdentifier(stereotypeContext.identifier());
            stereotypePtr.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.qualifiedName());
            stereotypePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext);
            return stereotypePtr;
        });
    }
}
