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

package org.finos.legend.engine.language.bigqueryFunction.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.BigQueryFunctionParserGrammar;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunctionDeploymentConfiguration;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.bigqueryFunction.metamodel.BigQueryFunction;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class BigQueryFunctionTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public BigQueryFunctionTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(BigQueryFunctionParserGrammar.DefinitionContext ctx)
    {
        if (ctx.bigQueryFunction() != null && !ctx.bigQueryFunction().isEmpty())
        {
            ctx.bigQueryFunction().stream().map(this::visitBigQueryFunction).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
        if (ctx.deploymentConfig() != null && !ctx.deploymentConfig().isEmpty())
        {
            ctx.deploymentConfig().stream().map(this::visitDeploymentConfig).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    private BigQueryFunctionDeploymentConfiguration visitDeploymentConfig(BigQueryFunctionParserGrammar.DeploymentConfigContext ctx)
    {
        BigQueryFunctionDeploymentConfiguration config = new BigQueryFunctionDeploymentConfiguration();
        ConnectionPointer pointer = new ConnectionPointer();
        pointer.connection = PureGrammarParserUtility.fromQualifiedName(ctx.activationConnection().qualifiedName().packagePath() == null
                ? Collections.emptyList() : ctx.activationConnection().qualifiedName().packagePath().identifier(), ctx.activationConnection().qualifiedName().identifier());
        pointer.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.activationConnection().qualifiedName());
        config.activationConnection = pointer;
        return config;
    }

    private BigQueryFunction visitBigQueryFunction(BigQueryFunctionParserGrammar.BigQueryFunctionContext ctx)
    {
        BigQueryFunction bigQueryFunction = new BigQueryFunction();
        bigQueryFunction.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        bigQueryFunction._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        bigQueryFunction.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        bigQueryFunction.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        bigQueryFunction.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        BigQueryFunctionParserGrammar.FunctionNameContext functionNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.functionName(), "functionName", bigQueryFunction.sourceInformation);
        bigQueryFunction.functionName = PureGrammarParserUtility.fromGrammarString(functionNameContext.STRING().getText(), true);
        BigQueryFunctionParserGrammar.FunctionContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.function(), "function", bigQueryFunction.sourceInformation);
        bigQueryFunction.function = new PackageableElementPointer(
            PackageableElementType.FUNCTION,
            functionContext.functionIdentifier().getText(),
            walkerSourceInformation.getSourceInformation(functionContext.functionIdentifier())
        );

        BigQueryFunctionParserGrammar.OwnerContext ownerContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.owner(), "owner", bigQueryFunction.sourceInformation);
        if (ownerContext != null)
        {
            bigQueryFunction.ownership = new DeploymentOwner(PureGrammarParserUtility.fromGrammarString(ownerContext.STRING().getText(), true));
        }
        BigQueryFunctionParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", bigQueryFunction.sourceInformation);
        if (descriptionContext != null)
        {
            bigQueryFunction.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }
        BigQueryFunctionParserGrammar.ActivationContext activationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.activation(), "activationConfiguration", bigQueryFunction.sourceInformation);
        if (activationContext != null)
        {
            BigQueryFunctionDeploymentConfiguration config = new BigQueryFunctionDeploymentConfiguration();
            ConnectionPointer connectionPointer = new ConnectionPointer();
            connectionPointer.connection = activationContext.qualifiedName().getText();
            config.activationConnection = connectionPointer;

            bigQueryFunction.activationConfiguration = config;
        }
        return bigQueryFunction;
    }

    private List<TaggedValue> visitTaggedValues(BigQueryFunctionParserGrammar.TaggedValuesContext ctx)
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

    private List<StereotypePtr> visitStereotypes(BigQueryFunctionParserGrammar.StereotypesContext ctx)
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
