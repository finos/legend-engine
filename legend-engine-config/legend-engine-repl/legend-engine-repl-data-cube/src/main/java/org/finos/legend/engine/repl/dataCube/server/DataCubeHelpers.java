// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.dataCube.server;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationTypeHelper;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.relationType.RelationType;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompleterExtension;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeExecutionResult;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_QUALIFIED_PATH;

public class DataCubeHelpers
{
    public static DataCubeExecutionResult executeQuery(Client client, LegendInterface legendInterface, PlanExecutor planExecutor, PureModelContextData data, boolean debug) throws IOException
    {

        Function func = (Function) ListIterate.select(data.getElements(), e -> e.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)).getFirst();
        String queryCode = getQueryCode(func.body.get(0), false);

        if (client != null && debug)
        {
            client.println("Debugging query execution...");
            client.printDebug("---------------------------------------- INPUT ----------------------------------------");
            client.println("Function: " + queryCode);
        }

        PureModel pureModel = legendInterface.compile(data);
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));

        // Plan
        if (client != null && debug)
        {
            client.printDebug("---------------------------------------- PLAN ----------------------------------------");
        }
        Root_meta_pure_executionPlan_ExecutionPlan _plan = legendInterface.generatePlan(pureModel, debug);
        String planStr = PlanGenerator.serializeToJSON(_plan, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers);
        if (client != null && debug)
        {
            client.println("Generated Plan: " + planStr);
        }

        // Execute
        Identity identity;
        try
        {
            identity = Identity.makeIdentity(SubjectTools.getLocalSubject());
        }
        catch (Exception e)
        {
            // Can't resolve identity from local subject
            identity = Identity.getAnonymousIdentity();
        }

        SingleExecutionPlan plan = (SingleExecutionPlan) PlanExecutor.readExecutionPlan(planStr);

        try (Result execResult = planExecutor.execute(plan, new HashMap<>(), identity.getName(), identity, null))
        {
            if (execResult instanceof RelationalResult)
            {
                if (client != null && debug)
                {
                    client.printDebug("---------------------------------------- RESULT ----------------------------------------");
                    client.println("Executed SQL: " + ((RelationalResult) execResult).executedSQl);
                }

                DataCubeExecutionResult result = new DataCubeExecutionResult();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ((RelationalResult) execResult).getSerializer(SerializationFormat.DEFAULT).stream(byteArrayOutputStream);
                result.result = byteArrayOutputStream.toString();
                result.executedQuery = queryCode;
                result.executedSQL = ((RelationalResult) execResult).executedSQl;
                return result;
            }
            throw new RuntimeException("Expected execution result of type 'RelationalResult', but got '" + execResult.getClass().getName() + "'");
        }
    }

    public static RelationType getRelationReturnType(LegendInterface legendInterface, PureModelContextData data)
    {
        PureModel pureModel = legendInterface.compile(data);
        return RelationTypeHelper.convert((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType) pureModel.getConcreteFunctionDefinition(REPL_RUN_FUNCTION_QUALIFIED_PATH, null)._expressionSequence().getLast()._genericType()._typeArguments().getFirst()._rawType());
    }

    public static ValueSpecification parseQuery(String code, Boolean returnSourceInformation)
    {
        return PureGrammarParser.newInstance().parseValueSpecification(code, "", 0, 0, returnSourceInformation != null && returnSourceInformation);
    }

    public static String getQueryCode(ValueSpecification valueSpecification, Boolean pretty)
    {
        return valueSpecification.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(pretty != null && pretty ? RenderStyle.PRETTY : RenderStyle.STANDARD).build());
    }

    public static CompletionResult getCodeTypeahead(String code, Boolean isPartial, PureModelContextData data, MutableList<CompleterExtension> extensions, LegendInterface legendInterface)
    {
        try
        {
            PureModelContextData newData = PureModelContextData.newBuilder()
                    .withOrigin(data.getOrigin())
                    .withSerializer(data.getSerializer())
                    .withElements(ListIterate.select(data.getElements(), el -> !el.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)))
                    .build();
            String graphCode = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).renderPureModelContextData(newData);
            String queryCode = code;
            if (isPartial != null && isPartial)
            {
                Function func = (Function) ListIterate.select(data.getElements(), el -> el.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)).getFirst();
                String existingCode = func.body.get(0).accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());
                queryCode = existingCode + code;
            }
            CompletionResult result = new Completer(graphCode, extensions, legendInterface).complete(queryCode);
            if (result.getEngineException() != null)
            {
                return new CompletionResult(Lists.mutable.empty());
            }
            return result;
        }
        catch (Exception e)
        {
            return new CompletionResult(Lists.mutable.empty());
        }
    }

    /**
     * Replace the magic function in the given graph data by a new function with the body of the specified lambda
     */
    public static Pair<PureModelContextData, Function> injectNewFunction(PureModelContextData originalData, Lambda lambda)
    {
        Function originalFunction = (Function) ListIterate.select(originalData.getElements(), e -> e.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)).getFirst();
        Function func = new Function();
        func.name = originalFunction.name;
        func._package = originalFunction._package;
        func.parameters = originalFunction.parameters;
        func.returnType = originalFunction.returnType;
        func.returnMultiplicity = originalFunction.returnMultiplicity;
        func.body = lambda != null ? lambda.body : func.body; // if no lambda is specified, we'll just use the original function

        PureModelContextData data = PureModelContextData.newBuilder()
                .withOrigin(originalData.getOrigin())
                .withSerializer(originalData.getSerializer())
                .withElements(ListIterate.select(originalData.getElements(), el -> !el.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)))
                .withElement(func)
                .build();

        return Tuples.pair(data, func);
    }
}
