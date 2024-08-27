//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.memsqlFunction.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MemSqlFunctionParserGrammar;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.memsqlFunction.metamodel.MemSqlFunction;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class MemSqlFunctionTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public MemSqlFunctionTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(MemSqlFunctionParserGrammar.DefinitionContext ctx)
    {
        if (ctx.memSqlFunction() != null && !ctx.memSqlFunction().isEmpty())
        {
            ctx.memSqlFunction().stream().map(this::visitMemSqlFunction).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
        if (ctx.deploymentConfig() != null && !ctx.deploymentConfig().isEmpty())
        {
            ctx.deploymentConfig().stream().map(this::visitDeploymentConfig).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    private MemSqlFunctionDeploymentConfiguration visitDeploymentConfig(MemSqlFunctionParserGrammar.DeploymentConfigContext ctx)
    {
        MemSqlFunctionDeploymentConfiguration config = new MemSqlFunctionDeploymentConfiguration();
        ConnectionPointer pointer = new ConnectionPointer();
        pointer.connection = PureGrammarParserUtility.fromQualifiedName(ctx.activationConnection().qualifiedName().packagePath() == null
                ? Collections.emptyList() : ctx.activationConnection().qualifiedName().packagePath().identifier(), ctx.activationConnection().qualifiedName().identifier());
        pointer.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.activationConnection().qualifiedName());
        config.activationConnection = pointer;
        return config;
    }

    private MemSqlFunction visitMemSqlFunction(MemSqlFunctionParserGrammar.MemSqlFunctionContext ctx)
    {
        MemSqlFunction memSqlFunction = new MemSqlFunction();
        memSqlFunction.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        memSqlFunction._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        memSqlFunction.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        memSqlFunction.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        memSqlFunction.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        MemSqlFunctionParserGrammar.FunctionNameContext functionNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.functionName(), "functionName", memSqlFunction.sourceInformation);
        memSqlFunction.functionName = PureGrammarParserUtility.fromGrammarString(functionNameContext.STRING().getText(), true);
        MemSqlFunctionParserGrammar.FunctionContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.function(), "function", memSqlFunction.sourceInformation);
        memSqlFunction.function = new PackageableElementPointer(
                PackageableElementType.FUNCTION,
                functionContext.functionIdentifier().getText(),
                walkerSourceInformation.getSourceInformation(functionContext.functionIdentifier()));

        MemSqlFunctionParserGrammar.OwnerContext ownerContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owner(), "owner", memSqlFunction.sourceInformation);
        if (ownerContext != null)
        {
            memSqlFunction.owner = PureGrammarParserUtility.fromGrammarString(ownerContext.STRING().getText(), true);
        }
        MemSqlFunctionParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", memSqlFunction.sourceInformation);
        if (descriptionContext != null)
        {
            memSqlFunction.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }
        MemSqlFunctionParserGrammar.ActivationContext activationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.activation(), "activationConfiguration", memSqlFunction.sourceInformation);
        if (activationContext != null)
        {
            MemSqlFunctionDeploymentConfiguration config = new MemSqlFunctionDeploymentConfiguration();
            ConnectionPointer connectionPointer = new ConnectionPointer();
            connectionPointer.connection = activationContext.qualifiedName().getText();
            config.activationConnection = connectionPointer;

            memSqlFunction.activationConfiguration = config;
        }
        return memSqlFunction;
    }

    private List<TaggedValue> visitTaggedValues(MemSqlFunctionParserGrammar.TaggedValuesContext ctx)
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

    private List<StereotypePtr> visitStereotypes(MemSqlFunctionParserGrammar.StereotypesContext ctx)
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
