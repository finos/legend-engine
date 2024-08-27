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

package org.finos.legend.engine.language.hostedService.grammar.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.antlr.v4.runtime.CharStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.HostedServiceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.runtime.RuntimeParser;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedService;
import org.finos.legend.engine.protocol.hostedService.metamodel.HostedServiceDeploymentConfiguration;
import org.finos.legend.engine.protocol.hostedService.metamodel.control.UserList;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.StereotypePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TagPtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionEnvironmentInstance;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionParameters;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class HostedServiceTreeWalker
{
    private final CharStream input;
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final Consumer<PackageableElement> elementConsumer;
    private final ImportAwareCodeSection section;
    private final PureGrammarParserContext context;

    public HostedServiceTreeWalker(CharStream input, ParseTreeWalkerSourceInformation walkerSourceInformation, Consumer<PackageableElement> elementConsumer, ImportAwareCodeSection section, PureGrammarParserContext context)
    {
        this.input = input;
        this.walkerSourceInformation = walkerSourceInformation;
        this.elementConsumer = elementConsumer;
        this.section = section;
        this.context = context;
    }

    public void visit(HostedServiceParserGrammar.DefinitionContext ctx)
    {
        if (ctx.service() != null && !ctx.service().isEmpty())
        {
            this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
            ctx.service().stream().map(this::visitHostedService).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
        if (ctx.execEnvs() != null && !ctx.execEnvs().isEmpty())
        {
            ctx.execEnvs().stream().map(this::visitExecutionEnvironment).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
        if (ctx.deploymentConfigs() != null && !ctx.deploymentConfigs().isEmpty())
        {
            ctx.deploymentConfigs().stream().map(this::visitDeploymentConfig).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    private HostedService visitHostedService(HostedServiceParserGrammar.ServiceContext ctx)
    {
        HostedService hostedService = new HostedService();
        hostedService.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        hostedService._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        hostedService.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        hostedService.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        hostedService.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        HostedServiceParserGrammar.ServicePatternContext patternContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.servicePattern(), "pattern", hostedService.sourceInformation);
        hostedService.pattern = PureGrammarParserUtility.fromGrammarString(patternContext.STRING().getText(), true);
        HostedServiceParserGrammar.ServiceFuncContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceFunc(), "function", hostedService.sourceInformation);
        hostedService.function = new PackageableElementPointer(
            PackageableElementType.FUNCTION,
            functionContext.functionIdentifier().getText(),
            walkerSourceInformation.getSourceInformation(functionContext.functionIdentifier())
        );
        HostedServiceParserGrammar.ServiceOwnershipContext ownerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceOwnership(), "ownership", hostedService.sourceInformation);
        if (ownerContext.userList() != null)
        {
            HostedServiceParserGrammar.UserListContext userListOwnersContext = ownerContext.userList();
            hostedService.ownership = new UserList(ListIterate.collect(userListOwnersContext.STRING(), ownerCtx -> PureGrammarParserUtility.fromGrammarString(ownerCtx.getText(), true)));
        }
        else
        {
            HostedServiceParserGrammar.DeploymentContext deploymentOwnerContext = ownerContext.deployment();
            hostedService.ownership = new DeploymentOwner(PureGrammarParserUtility.fromGrammarString(deploymentOwnerContext.STRING().getText(), true));
        }
        HostedServiceParserGrammar.ServiceDocumentationContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceDocumentation(), "documentation", hostedService.sourceInformation);
        if (descriptionContext != null)
        {
            hostedService.documentation = PureGrammarParserUtility.fromGrammarString(descriptionContext.STRING().getText(), true);
        }
        HostedServiceParserGrammar.ServiceAutoActivateUpdatesContext autoActivateUpdatesContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceAutoActivateUpdates(), "autoActivateUpdates", hostedService.sourceInformation);
        hostedService.autoActivateUpdates = autoActivateUpdatesContext != null && Boolean.parseBoolean(autoActivateUpdatesContext.BOOLEAN().getText());

        return hostedService;
    }

    private List<TaggedValue> visitTaggedValues(HostedServiceParserGrammar.TaggedValuesContext ctx)
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

    private List<StereotypePtr> visitStereotypes(HostedServiceParserGrammar.StereotypesContext ctx)
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

    private HostedServiceDeploymentConfiguration visitDeploymentConfig(HostedServiceParserGrammar.DeploymentConfigsContext ctx)
    {
        HostedServiceDeploymentConfiguration config = new HostedServiceDeploymentConfiguration();
        return config;
    }

    //execution environment parsing
    private ExecutionEnvironmentInstance visitExecutionEnvironment(HostedServiceParserGrammar.ExecEnvsContext ctx)
    {
        ExecutionEnvironmentInstance execEnv = new ExecutionEnvironmentInstance();
        execEnv.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        execEnv._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        execEnv.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        List<HostedServiceParserGrammar.ExecParamsContext> execEnvCtxList = PureGrammarParserUtility.validateRequiredListField(ctx.executions().execParams(), "executions", walkerSourceInformation.getSourceInformation(ctx.executions()));
        if (execEnvCtxList.stream().anyMatch(x -> x.singleExecEnv() != null))
        {
            execEnv.executionParameters = ListIterate.collect(execEnvCtxList, execEnvContext -> this.visitSingleExecutionParameters(execEnvContext.singleExecEnv()));
        }
        else if (execEnvCtxList.stream().anyMatch(x -> x.multiExecEnv() != null))
        {
            execEnv.executionParameters = ListIterate.collect(execEnvCtxList, execEnvContext -> this.visitMultiExecutionParameters(execEnvContext.multiExecEnv()));
        }
        else
        {
            throw new EngineException("Valid types for ExecutionEnvironment are: Single, Multi");
        }
        return execEnv;
    }

    private SingleExecutionParameters visitSingleExecutionParameters(HostedServiceParserGrammar.SingleExecEnvContext ctx)
    {
        SingleExecutionParameters singleExecParams = new SingleExecutionParameters();
        singleExecParams.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        singleExecParams.key = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        HostedServiceParserGrammar.ServiceMappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(Collections.singletonList(ctx.serviceMapping()), "mapping", singleExecParams.sourceInformation);
        singleExecParams.mapping = PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier());
        singleExecParams.mappingSourceInformation = walkerSourceInformation.getSourceInformation(mappingContext.qualifiedName());
        // runtime
        HostedServiceParserGrammar.ServiceRuntimeContext runtimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(Collections.singletonList(ctx.serviceRuntime()), "runtime", singleExecParams.sourceInformation);
        singleExecParams.runtime = this.visitRuntime(runtimeContext);
        return singleExecParams;
    }

    private MultiExecutionParameters visitMultiExecutionParameters(HostedServiceParserGrammar.MultiExecEnvContext ctx)
    {
        MultiExecutionParameters multiExecParams = new MultiExecutionParameters();
        multiExecParams.masterKey = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        List<HostedServiceParserGrammar.SingleExecEnvContext> singleExecCtxList = PureGrammarParserUtility.validateRequiredListField(ctx.singleExecEnv(), "executions", walkerSourceInformation.getSourceInformation(ctx));
        multiExecParams.singleExecutionParameters = ListIterate.collect(singleExecCtxList, this::visitSingleExecutionParameters);
        return multiExecParams;
    }

    private Runtime visitRuntime(HostedServiceParserGrammar.ServiceRuntimeContext serviceRuntimeContext)
    {
        if (serviceRuntimeContext.runtimePointer() != null)
        {
            RuntimePointer runtimePointer = new RuntimePointer();
            if (serviceRuntimeContext.runtimePointer().qualifiedName() != null)
            {
                runtimePointer.runtime = PureGrammarParserUtility.fromQualifiedName(serviceRuntimeContext.runtimePointer().qualifiedName().packagePath() == null ? Collections.emptyList() : serviceRuntimeContext.runtimePointer().qualifiedName().packagePath().identifier(), serviceRuntimeContext.runtimePointer().qualifiedName().identifier());
                runtimePointer.sourceInformation = walkerSourceInformation.getSourceInformation(serviceRuntimeContext.runtimePointer().qualifiedName());
            }
            return runtimePointer;
        }
        else if (serviceRuntimeContext.embeddedRuntime() != null)
        {
            StringBuilder embeddedRuntimeText = new StringBuilder();
            for (HostedServiceParserGrammar.EmbeddedRuntimeContentContext fragment : serviceRuntimeContext.embeddedRuntime().embeddedRuntimeContent())
            {
                embeddedRuntimeText.append(fragment.getText());
            }
            String embeddedRuntimeParsingText = embeddedRuntimeText.length() > 0 ? embeddedRuntimeText.substring(0, embeddedRuntimeText.length() - 2) : embeddedRuntimeText.toString();
            RuntimeParser runtimeParser = RuntimeParser.newInstance(this.context.getPureGrammarParserExtensions());
            // prepare island grammar walker source information
            int startLine = serviceRuntimeContext.embeddedRuntime().ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + serviceRuntimeContext.embeddedRuntime().ISLAND_OPEN().getSymbol().getCharPositionInLine() + serviceRuntimeContext.embeddedRuntime().ISLAND_OPEN().getText().length();
            ParseTreeWalkerSourceInformation embeddedRuntimeWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
            SourceInformation embeddedRuntimeSourceInformation = walkerSourceInformation.getSourceInformation(serviceRuntimeContext.embeddedRuntime());
            return runtimeParser.parseEmbeddedRuntime(embeddedRuntimeParsingText, embeddedRuntimeWalkerSourceInformation, embeddedRuntimeSourceInformation);
        }
        throw new UnsupportedOperationException();
    }

}
