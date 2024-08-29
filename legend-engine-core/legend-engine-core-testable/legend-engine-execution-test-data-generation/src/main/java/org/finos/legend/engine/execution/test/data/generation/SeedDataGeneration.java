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

package org.finos.legend.engine.execution.test.data.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.builder.relation.RelationBuilder;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.ColumnValuePair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.RowIdentifier;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class SeedDataGeneration
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SeedDataGeneration.class);
    private static final ObjectMapper objectMapper = TestDataGenerationObjectMapperFactory.getNewObjectMapper();


    public static Response executeSeedDataGenerate(Function<PureModel, LambdaFunction> functionFunc, Function0<PureModel> pureModelFunc, String mapping, Runtime runtime, ExecutionContext context, Map<String, Object> parameterNameValueMap, String clientVersion, Identity identity, String user, PlanExecutor planExecutor)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_SEED_DATA_START, "").toString());

            PureModel pureModel = pureModelFunc.value();
            LambdaFunction<?> lambdaFunction = functionFunc.valueOf(pureModel);
            Mapping pureMapping = pureModel.getMapping(mapping);
            Root_meta_core_runtime_Runtime pureRuntime = HelperRuntimeBuilder.buildPureRuntime(runtime, pureModel.getContext());
            Root_meta_pure_runtime_ExecutionContext executionContext = context == null ? new Root_meta_pure_runtime_ExecutionContext_Impl("") : HelperValueSpecificationBuilder.processExecutionContext(context, pureModel.getContext());

            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
            MutableList<PlanGeneratorExtension> planGeneratorExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
            MutableList<PlanTransformer> planTransformers = planGeneratorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

            SingleExecutionPlan plan = PlanGenerator.transformExecutionPlan(generateSeedDataPlan(lambdaFunction, pureMapping, pureRuntime, executionContext, parameterNameValueMap, pureModel, routerExtensions, identity), pureModel, clientVersion, identity, routerExtensions, planTransformers);
            Result result = planExecutor.execute(plan, Maps.mutable.empty(), user, identity);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_SEED_DATA_STOP, System.currentTimeMillis() - start).toString());
            return manageSeedDataResult(identity, result, LoggingEventType.GENERATE_SEED_DATA_ERROR);
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_SEED_DATA_ERROR, identity.getName());
        }
    }


    public static Root_meta_pure_executionPlan_ExecutionPlan generateSeedDataPlan(LambdaFunction<?> lambdaFunction, Mapping mapping, Root_meta_core_runtime_Runtime runtime, Root_meta_pure_runtime_ExecutionContext executionContext, Map<String, Object> parameterNameValueMap, PureModel pureModel, RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions, Identity identity)
    {
        Root_meta_pure_executionPlan_ExecutionPlan plan;
        try (Scope scope = GlobalTracer.get().buildSpan("Generate Plan").startActive(true))
        {
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<String, Root_meta_pure_functions_collection_List_Impl<Object>>> parameterNameValuePair = FastList.newList();
            for (Map.Entry<String, ?> e : parameterNameValueMap.entrySet())
            {
                Root_meta_pure_functions_collection_List_Impl<Object> lis = new Root_meta_pure_functions_collection_List_Impl<Object>("");
                lis._valuesAdd(e.getValue());
                parameterNameValuePair.add(newPair(e.getKey(), lis));
            }
            plan = core_relational_relational_testDataGeneration_testDataGeneration.Root_meta_relational_testDataGeneration_executionPlan_planSeedDataGenerationWithParameterNameValuePairs_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_1__Pair_MANY__Extension_MANY__ExecutionPlan_1_(
                    lambdaFunction,
                    mapping,
                    runtime,
                    executionContext,
                    parameterNameValuePair,
                    routerExtensions,
                    pureModel.getExecutionSupport()
            );
            plan = PlanPlatform.JAVA.bindPlan(plan, pureModel, routerExtensions);
            String stringPlan = core_pure_executionPlan_executionPlan_print.Root_meta_pure_executionPlan_toString_planToString_ExecutionPlan_1__Extension_MANY__String_1_(plan, routerExtensions, pureModel.getExecutionSupport());
            scope.span().log(String.valueOf(LoggingEventType.PLAN_GENERATED));
            scope.span().setTag("plan", stringPlan);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.PLAN_GENERATED, stringPlan).toString());
        }
        return plan;
    }

    private static Response manageSeedDataResult(Identity identity, Result result, LoggingEventType loggingEventType)
    {
        if (result instanceof ErrorResult)
        {
            String message = ((ErrorResult) result).getMessage();
            LOGGER.info(new LogInfo(identity.getName(), loggingEventType, message).toString());
            return Response.status(500).entity(message).build();
        }
        else if (result instanceof RelationalResult && ((RelationalResult) result).getResultBuilder() instanceof RelationBuilder)
        {
            return Response.ok((StreamingOutput) outputStream -> streamSeedDataResult((RelationalResult) result, outputStream)).build();
        }
        else
        {
            throw new RuntimeException("Unexpected Result :" + result.getClass().getName());
        }
    }

    private static void streamSeedDataResult(RelationalResult relationalResult, OutputStream stream)
    {
        try
        {
            RelationBuilder relationBuilder = (RelationBuilder) relationalResult.getResultBuilder();
            stream.write("{\"table\":{".getBytes());
            stream.write("\"_type\":\"table\",".getBytes());
            stream.write(("\"table\":\"" + relationBuilder.relationName + "\",").getBytes());
            stream.write(("\"schema\":\"" + relationBuilder.schemaName + "\",").getBytes());
            stream.write(("\"database\":\"" + relationBuilder.database + "\"").getBytes());
            stream.write("}".getBytes());
            stream.write(",\"rowIdentifiers\":[".getBytes());
            if (relationalResult.resultSet.next())
            {
                objectMapper.writeValue(stream, getRowIdentifierFromNextRow(relationalResult));
            }
            while (relationalResult.resultSet.next())
            {
                stream.write(",".getBytes());
                objectMapper.writeValue(stream, getRowIdentifierFromNextRow(relationalResult));
            }
            stream.write("]}".getBytes());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            relationalResult.close();
        }
    }


    static RowIdentifier getRowIdentifierFromNextRow(RelationalResult relationalResult) throws SQLException
    {
        List<String> sqlColumns = ListIterate.collect(relationalResult.getSQLResultColumns(), SQLResultColumn::getQuotedLabelIfContainSpace);
        RowIdentifier rowIdentifier = new RowIdentifier();
        rowIdentifier.columnValuePairs = Lists.mutable.empty();
        int index = 1;
        for (String column : sqlColumns)
        {
            ColumnValuePair cvPair = new ColumnValuePair();
            cvPair.name = column;
            cvPair.value = relationalResult.getTransformers().get(index - 1).valueOf(relationalResult.getValue(index));
            rowIdentifier.columnValuePairs.add(cvPair);
            index += 1;
        }
        return rowIdentifier;
    }

    private static <T, U> Pair<T, U> newPair(T first, U second)
    {
        return new Root_meta_pure_functions_collection_Pair_Impl<T, U>("")._first(first)._second(second);
    }
}