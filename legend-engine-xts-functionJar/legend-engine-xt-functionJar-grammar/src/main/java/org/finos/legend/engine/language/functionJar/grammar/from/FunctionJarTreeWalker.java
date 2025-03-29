//  Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.functionJar.grammar.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FunctionJarParserGrammar;
import org.finos.legend.engine.protocol.functionJar.metamodel.FunctionJar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class FunctionJarTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserContext context;

    public FunctionJarTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section, PureGrammarParserContext context)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.context = context;
    }

    public void visit(FunctionJarParserGrammar.DefinitionContext ctx)
    {
        if (ctx.functionJar() != null && !ctx.functionJar().isEmpty())
        {
            this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
            ctx.functionJar().stream().map(this::visitFunctionJar).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    private FunctionJar visitFunctionJar(FunctionJarParserGrammar.FunctionJarContext ctx)
    {
        FunctionJar functionJar = new FunctionJar();
        functionJar.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        functionJar._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        functionJar.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        functionJar.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        functionJar.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        FunctionJarParserGrammar.FunctionJarFuncContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.functionJarFunc(), "function", functionJar.sourceInformation);
        functionJar.function = new PackageableElementPointer(
                PackageableElementType.FUNCTION,
                functionContext.functionIdentifier().getText(),
                walkerSourceInformation.getSourceInformation(functionContext.functionIdentifier())
        );
        FunctionJarParserGrammar.FunctionJarOwnershipContext ownerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.functionJarOwnership(), "ownership", functionJar.sourceInformation);
        FunctionJarParserGrammar.DeploymentContext deploymentOwnerContext = ownerContext.deployment();
        functionJar.ownership = new DeploymentOwner(PureGrammarParserUtility.fromGrammarString(deploymentOwnerContext.STRING().getText(), true));

        FunctionJarParserGrammar.FunctionJarDocumentationContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.functionJarDocumentation(), "documentation", functionJar.sourceInformation);
        if (descriptionContext != null)
        {
            functionJar.documentation = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }

        return functionJar;
    }

    private List<TaggedValue> visitTaggedValues(FunctionJarParserGrammar.TaggedValuesContext ctx)
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

    private List<StereotypePtr> visitStereotypes(FunctionJarParserGrammar.StereotypesContext ctx)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext ->
        {
            StereotypePtr stereotypePtr = new StereotypePtr();
            stereotypePtr.profile = PureGrammarParserUtility.fromQualifiedName(stereotypeContext.qualifiedName().packagePath() == null ? Collections.emptyList() : stereotypeContext.qualifiedName().packagePath().identifier(), stereotypeContext.qualifiedName().identifier());
            stereotypePtr.value = PureGrammarParserUtility.fromIdentifier(stereotypeContext.identifier());
            stereotypePtr.profileSourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.qualifiedName());
            stereotypePtr.sourceInformation = this.walkerSourceInformation.getSourceInformation(stereotypeContext.identifier());
            return stereotypePtr;
        });
    }
}
