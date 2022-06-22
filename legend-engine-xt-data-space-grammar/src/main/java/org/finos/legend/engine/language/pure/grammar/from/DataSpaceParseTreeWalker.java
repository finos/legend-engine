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

import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DataSpaceParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpace;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.dataSpace.DataSpaceExecutionContext;
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
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public DataSpaceParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
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
        dataSpace.executionContexts = ListIterate.collect(executionContextsContext.executionContext(), executionContext -> this.visitDataSpaceExecutionContext(executionContext, dataSpace.sourceInformation));
        // Default execution context
        DataSpaceParserGrammar.DefaultExecutionContextContext defaultExecutionContextContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.defaultExecutionContext(), "defaultExecutionContext", dataSpace.sourceInformation);
        dataSpace.defaultExecutionContext = PureGrammarParserUtility.fromGrammarString(defaultExecutionContextContext.STRING().getText(), true);

        // Description (optional)
        DataSpaceParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", dataSpace.sourceInformation);
        dataSpace.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;

        // Featured diagrams (optional)
        DataSpaceParserGrammar.FeaturedDiagramsContext featuredDiagramsContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.featuredDiagrams(), "featuredDiagrams", dataSpace.sourceInformation);
        dataSpace.featuredDiagrams = featuredDiagramsContext != null ? ListIterate.collect(featuredDiagramsContext.qualifiedName(), diagramPathContext ->
        {
            PackageableElementPointer pointer = new PackageableElementPointer(
                    PackageableElementType.DIAGRAM,
                    PureGrammarParserUtility.fromQualifiedName(diagramPathContext.packagePath() == null ? Collections.emptyList() : diagramPathContext.packagePath().identifier(), diagramPathContext.identifier())
            );
            pointer.sourceInformation = walkerSourceInformation.getSourceInformation(diagramPathContext);
            return pointer;
        }) : null;

        // Support info (optional)
        DataSpaceParserGrammar.SupportInfoContext supportInfoContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.supportInfo(), "supportInfo", dataSpace.sourceInformation);
        dataSpace.supportInfo = supportInfoContext != null ? this.visitDataSpaceSupportInfo(supportInfoContext, dataSpace.sourceInformation) : null;
        return dataSpace;
    }

    private DataSpaceExecutionContext visitDataSpaceExecutionContext(DataSpaceParserGrammar.ExecutionContextContext ctx, SourceInformation dataSpaceSourceInformation)
    {
        DataSpaceExecutionContext executionContext = new DataSpaceExecutionContext();
        // Name
        DataSpaceParserGrammar.ExecutionContextNameContext executionContextNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.executionContextName(), "name", dataSpaceSourceInformation);
        executionContext.name = PureGrammarParserUtility.fromGrammarString(executionContextNameContext.STRING().getText(), true);
        // Description (optional)
        DataSpaceParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", dataSpaceSourceInformation);
        executionContext.description = descriptionContext != null ? PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true) : null;
        // Mapping
        DataSpaceParserGrammar.MappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.mapping(), "mapping", dataSpaceSourceInformation);
        executionContext.mapping = new PackageableElementPointer(
                PackageableElementType.MAPPING,
                PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier())
        );
        executionContext.mapping.sourceInformation = walkerSourceInformation.getSourceInformation(mappingContext);
        // Runtime
        DataSpaceParserGrammar.DefaultRuntimeContext defaultRuntimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.defaultRuntime(), "defaultRuntime", dataSpaceSourceInformation);
        executionContext.defaultRuntime = new PackageableElementPointer(
                PackageableElementType.RUNTIME,
                PureGrammarParserUtility.fromQualifiedName(defaultRuntimeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : defaultRuntimeContext.qualifiedName().packagePath().identifier(), defaultRuntimeContext.qualifiedName().identifier())
        );
        executionContext.defaultRuntime.sourceInformation = walkerSourceInformation.getSourceInformation(defaultRuntimeContext);
        return executionContext;
    }

    // NOTE: for simplicity reason, in the grammar, we only support email address as the only support info type at the moment
    // when there are more, we will handle the extension mechanism later
    private DataSpaceSupportInfo visitDataSpaceSupportInfo(DataSpaceParserGrammar.SupportInfoContext ctx, SourceInformation dataSpaceSourceInformation)
    {
        DataSpaceSupportEmail supportInfo = new DataSpaceSupportEmail();
        // Email
        DataSpaceParserGrammar.SupportEmailContext supportEmailContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.supportEmail(), "address", dataSpaceSourceInformation);
        supportInfo.address = PureGrammarParserUtility.fromGrammarString(supportEmailContext.STRING().getText(), true);
        return supportInfo;
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
