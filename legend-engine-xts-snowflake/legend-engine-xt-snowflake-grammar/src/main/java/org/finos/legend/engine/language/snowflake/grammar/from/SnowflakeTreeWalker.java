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

package org.finos.legend.engine.language.snowflake.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.SnowflakeParserGrammar;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeApp;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakeAppDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflake.snowflakeApp.metamodel.SnowflakePermissionScheme;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdf;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdfDeploymentConfiguration;
import org.finos.legend.engine.protocol.snowflake.snowflakeM2MUdf.metamodel.SnowflakeM2MUdfPermissionScheme;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class SnowflakeTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final DefaultCodeSection section;

    public SnowflakeTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, DefaultCodeSection section)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
    }

    public void visit(SnowflakeParserGrammar.DefinitionContext ctx)
    {
        if (ctx.snowflakeApp() != null && !ctx.snowflakeApp().isEmpty())
        {
            ctx.snowflakeApp().stream().map(this::visitSnowflakeApp).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }

        if (ctx.snowflakeM2MUdf() != null && !ctx.snowflakeM2MUdf().isEmpty())
        {
            ctx.snowflakeM2MUdf().stream().map(this::visitSnowflakeM2MUdf).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    private SnowflakeApp visitSnowflakeApp(SnowflakeParserGrammar.SnowflakeAppContext ctx)
    {
        SnowflakeApp snowflakeApp = new SnowflakeApp();
        snowflakeApp.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        snowflakeApp._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        snowflakeApp.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        snowflakeApp.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        snowflakeApp.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        SnowflakeParserGrammar.ApplicationNameContext applicationNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.applicationName(), "applicationName", snowflakeApp.sourceInformation);
        snowflakeApp.applicationName = PureGrammarParserUtility.fromGrammarString(applicationNameContext.STRING().getText(), true);
        SnowflakeParserGrammar.FunctionContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.function(), "function", snowflakeApp.sourceInformation);
        snowflakeApp.function = new PackageableElementPointer(
                PackageableElementType.FUNCTION,
                functionContext.functionIdentifier().getText(),
                walkerSourceInformation.getSourceInformation(functionContext.functionIdentifier())
        );
        SnowflakeParserGrammar.OwnershipContext ownerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.ownership(), "ownership", snowflakeApp.sourceInformation);
        snowflakeApp.ownership = new DeploymentOwner(PureGrammarParserUtility.fromGrammarString(ownerContext.STRING().getText(), true));
        SnowflakeParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", snowflakeApp.sourceInformation);
        if (descriptionContext != null)
        {
            snowflakeApp.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }
        SnowflakeParserGrammar.RoleContext roleContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.role(), "role", snowflakeApp.sourceInformation);
        if (roleContext != null)
        {
           snowflakeApp.usageRole = PureGrammarParserUtility.fromGrammarString(roleContext.STRING().getText(), true);
        }
        SnowflakeParserGrammar.SchemeContext schemeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.scheme(), "scheme", snowflakeApp.sourceInformation);
        if (schemeContext != null)
        {
            try
            {
                snowflakeApp.permissionScheme = SnowflakePermissionScheme.valueOf(PureGrammarParserUtility.fromIdentifier(schemeContext.identifier()));
            }
            catch (Exception e)
            {
                throw new EngineException("Unknown permission scheme '" + PureGrammarParserUtility.fromIdentifier(schemeContext.identifier()) + "'", this.walkerSourceInformation.getSourceInformation(schemeContext), EngineErrorType.PARSER);
            }
        }
        SnowflakeParserGrammar.DeploymentSchemaContext deploymentSchemaContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.deploymentSchema(), "deploymentSchema", snowflakeApp.sourceInformation);
        if (deploymentSchemaContext != null)
        {
            snowflakeApp.deploymentSchema = PureGrammarParserUtility.fromGrammarString(deploymentSchemaContext.STRING().getText(), true);
        }
        SnowflakeParserGrammar.ActivationContext activationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.activation(), "activation", snowflakeApp.sourceInformation);
        if (activationContext != null)
        {
            ConnectionPointer p = new ConnectionPointer();
            p.connection = PureGrammarParserUtility.fromQualifiedName(activationContext.qualifiedName().packagePath() == null
                    ? Collections.emptyList() : activationContext.qualifiedName().packagePath().identifier(), activationContext.qualifiedName().identifier());
            p.sourceInformation = walkerSourceInformation.getSourceInformation(activationContext.qualifiedName());
            snowflakeApp.activationConfiguration = new SnowflakeAppDeploymentConfiguration(p);
        }
        return snowflakeApp;
    }

    private SnowflakeM2MUdf visitSnowflakeM2MUdf(SnowflakeParserGrammar.SnowflakeM2MUdfContext ctx)
    {
        SnowflakeM2MUdf SnowflakeM2MUdf = new SnowflakeM2MUdf();
        SnowflakeM2MUdf.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        SnowflakeM2MUdf._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        SnowflakeM2MUdf.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        SnowflakeM2MUdf.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        SnowflakeM2MUdf.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        SnowflakeParserGrammar.UdfNameContext udfNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.udfName(), "udfName", SnowflakeM2MUdf.sourceInformation);
        SnowflakeM2MUdf.udfName = PureGrammarParserUtility.fromGrammarString(udfNameContext.STRING().getText(), true);
        SnowflakeParserGrammar.FunctionContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.function(), "function", SnowflakeM2MUdf.sourceInformation);
        SnowflakeM2MUdf.function = new PackageableElementPointer(
                PackageableElementType.FUNCTION,
                functionContext.functionIdentifier().getText(),
                walkerSourceInformation.getSourceInformation(functionContext.functionIdentifier())
        );
        SnowflakeParserGrammar.OwnershipContext ownerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.ownership(), "ownership", SnowflakeM2MUdf.sourceInformation);
        SnowflakeM2MUdf.ownership = new DeploymentOwner(PureGrammarParserUtility.fromGrammarString(ownerContext.STRING().getText(), true));
        SnowflakeParserGrammar.DescriptionContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.description(), "description", SnowflakeM2MUdf.sourceInformation);
        if (descriptionContext != null)
        {
            SnowflakeM2MUdf.description = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }
        SnowflakeParserGrammar.RoleContext roleContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.role(), "role", SnowflakeM2MUdf.sourceInformation);
        if (roleContext != null)
        {
            SnowflakeM2MUdf.usageRole = PureGrammarParserUtility.fromGrammarString(roleContext.STRING().getText(), true);
        }
        SnowflakeParserGrammar.SchemeContext schemeContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.scheme(), "scheme", SnowflakeM2MUdf.sourceInformation);
        if (schemeContext != null)
        {
            try
            {
                SnowflakeM2MUdf.permissionScheme = SnowflakeM2MUdfPermissionScheme.valueOf(PureGrammarParserUtility.fromIdentifier(schemeContext.identifier()));
            }
            catch (Exception e)
            {
                throw new EngineException("Unknown permission scheme '" + PureGrammarParserUtility.fromIdentifier(schemeContext.identifier()) + "'", this.walkerSourceInformation.getSourceInformation(schemeContext), EngineErrorType.PARSER);
            }
        }
        SnowflakeParserGrammar.DeploymentSchemaContext deploymentSchemaContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deploymentSchema(), "deploymentSchema", SnowflakeM2MUdf.sourceInformation);
        SnowflakeM2MUdf.deploymentSchema = PureGrammarParserUtility.fromGrammarString(deploymentSchemaContext.STRING().getText(), true);
        SnowflakeParserGrammar.DeploymentStageContext deploymentStageContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.deploymentStage(), "deploymentStage", SnowflakeM2MUdf.sourceInformation);
        SnowflakeM2MUdf.deploymentStage = PureGrammarParserUtility.fromGrammarString(deploymentStageContext.STRING().getText(), true);
        SnowflakeParserGrammar.ActivationContext activationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.activation(), "activation", SnowflakeM2MUdf.sourceInformation);
        if (activationContext != null)
        {
            ConnectionPointer p = new ConnectionPointer();
            p.connection = PureGrammarParserUtility.fromQualifiedName(activationContext.qualifiedName().packagePath() == null
                    ? Collections.emptyList() : activationContext.qualifiedName().packagePath().identifier(), activationContext.qualifiedName().identifier());
            p.sourceInformation = walkerSourceInformation.getSourceInformation(activationContext.qualifiedName());
            SnowflakeM2MUdf.activationConfiguration = new SnowflakeM2MUdfDeploymentConfiguration(p);
        }
        return SnowflakeM2MUdf;
    }

    private List<TaggedValue> visitTaggedValues(SnowflakeParserGrammar.TaggedValuesContext ctx)
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

    private List<StereotypePtr> visitStereotypes(SnowflakeParserGrammar.StereotypesContext ctx)
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
