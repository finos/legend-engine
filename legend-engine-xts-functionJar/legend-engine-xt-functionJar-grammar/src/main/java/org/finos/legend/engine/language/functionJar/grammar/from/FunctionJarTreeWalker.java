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
import org.finos.legend.engine.language.pure.grammar.from.runtime.RuntimeParser;
import org.finos.legend.engine.protocol.functionJar.metamodel.FunctionJar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.functionActivator.metamodel.DeploymentOwner;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.m3.extension.StereotypePtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TagPtr;
import org.finos.legend.engine.protocol.pure.m3.extension.TaggedValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ExecutionEnvironmentInstance;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.MultiExecutionParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.SingleExecutionParameters;
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
        if (ctx.service() != null && !ctx.service().isEmpty())
        {
            this.section.imports = ListIterate.collect(ctx.imports().importStatement(), importCtx -> PureGrammarParserUtility.fromPath(importCtx.packagePath().identifier()));
            ctx.service().stream().map(this::visitFunctionJar).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
        if (ctx.execEnvs() != null && !ctx.execEnvs().isEmpty())
        {
            ctx.execEnvs().stream().map(this::visitExecutionEnvironment).peek(e -> this.section.elements.add(e.getPath())).forEach(this.elementConsumer);
        }
    }

    private FunctionJar visitFunctionJar(FunctionJarParserGrammar.ServiceContext ctx)
    {
        FunctionJar functionJar = new FunctionJar();
        functionJar.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        functionJar._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        functionJar.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        functionJar.stereotypes = ctx.stereotypes() == null ? Lists.mutable.empty() : this.visitStereotypes(ctx.stereotypes());
        functionJar.taggedValues = ctx.taggedValues() == null ? Lists.mutable.empty() : this.visitTaggedValues(ctx.taggedValues());

        FunctionJarParserGrammar.ServiceFuncContext functionContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceFunc(), "function", functionJar.sourceInformation);
        functionJar.function = new PackageableElementPointer(
                PackageableElementType.FUNCTION,
                functionContext.functionIdentifier().getText(),
                walkerSourceInformation.getSourceInformation(functionContext.functionIdentifier())
        );
        FunctionJarParserGrammar.ServiceOwnershipContext ownerContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.serviceOwnership(), "ownership", functionJar.sourceInformation);
        FunctionJarParserGrammar.DeploymentContext deploymentOwnerContext = ownerContext.deployment();
        functionJar.ownership = new DeploymentOwner(PureGrammarParserUtility.fromGrammarString(deploymentOwnerContext.STRING().getText(), true));

        FunctionJarParserGrammar.ServiceDocumentationContext descriptionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.serviceDocumentation(), "documentation", functionJar.sourceInformation);
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

    //execution environment parsing
    private ExecutionEnvironmentInstance visitExecutionEnvironment(FunctionJarParserGrammar.ExecEnvsContext ctx)
    {
        ExecutionEnvironmentInstance execEnv = new ExecutionEnvironmentInstance();
        execEnv.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        execEnv._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        execEnv.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        List<FunctionJarParserGrammar.ExecParamsContext> execEnvCtxList = PureGrammarParserUtility.validateRequiredListField(ctx.executions().execParams(), "executions", walkerSourceInformation.getSourceInformation(ctx.executions()));
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

    private SingleExecutionParameters visitSingleExecutionParameters(FunctionJarParserGrammar.SingleExecEnvContext ctx)
    {
        SingleExecutionParameters singleExecParams = new SingleExecutionParameters();
        singleExecParams.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        singleExecParams.key = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        FunctionJarParserGrammar.ServiceMappingContext mappingContext = PureGrammarParserUtility.validateAndExtractRequiredField(Collections.singletonList(ctx.serviceMapping()), "mapping", singleExecParams.sourceInformation);
        singleExecParams.mapping = PureGrammarParserUtility.fromQualifiedName(mappingContext.qualifiedName().packagePath() == null ? Collections.emptyList() : mappingContext.qualifiedName().packagePath().identifier(), mappingContext.qualifiedName().identifier());
        singleExecParams.mappingSourceInformation = walkerSourceInformation.getSourceInformation(mappingContext.qualifiedName());
        // runtime
        FunctionJarParserGrammar.ServiceRuntimeContext runtimeContext = PureGrammarParserUtility.validateAndExtractRequiredField(Collections.singletonList(ctx.serviceRuntime()), "runtime", singleExecParams.sourceInformation);
        singleExecParams.runtime = this.visitRuntime(runtimeContext);
        return singleExecParams;
    }

    private MultiExecutionParameters visitMultiExecutionParameters(FunctionJarParserGrammar.MultiExecEnvContext ctx)
    {
        MultiExecutionParameters multiExecParams = new MultiExecutionParameters();
        multiExecParams.masterKey = PureGrammarParserUtility.fromIdentifier(ctx.identifier());
        List<FunctionJarParserGrammar.SingleExecEnvContext> singleExecCtxList = PureGrammarParserUtility.validateRequiredListField(ctx.singleExecEnv(), "executions", walkerSourceInformation.getSourceInformation(ctx));
        multiExecParams.singleExecutionParameters = ListIterate.collect(singleExecCtxList, this::visitSingleExecutionParameters);
        return multiExecParams;
    }

    private Runtime visitRuntime(FunctionJarParserGrammar.ServiceRuntimeContext serviceRuntimeContext)
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
            for (FunctionJarParserGrammar.EmbeddedRuntimeContentContext fragment : serviceRuntimeContext.embeddedRuntime().embeddedRuntimeContent())
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
