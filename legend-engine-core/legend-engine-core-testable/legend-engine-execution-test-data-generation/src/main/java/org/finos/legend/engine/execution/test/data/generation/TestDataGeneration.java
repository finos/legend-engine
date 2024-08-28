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
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperTestDataGenerationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ErrorResult;
import org.finos.legend.engine.plan.execution.result.MultiResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.builder.relation.RelationBuilder;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.RelationType;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.executionContext.ExecutionContext;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.ColumnValuePair;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.RowIdentifier;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.testDataGeneration.TableRowIdentifiers;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionTool;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;

public class TestDataGeneration
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TestDataGeneration.class);
    private static final ObjectMapper objectMapper = TestDataGenerationObjectMapperFactory.getNewObjectMapper();

    public static Response executeTestDataGenerateWithSeed(Function<PureModel, LambdaFunction> functionFunc, Function0<PureModel> pureModelFunc, String mapping, Runtime runtime, ExecutionContext context, List<TableRowIdentifiers> tableRowIdentifiers, Boolean hashStrings, Map<String, Object> parameterNameValueMap, String clientVersion, Identity identity, String user, PlanExecutor planExecutor)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_TEST_DATA_START, "").toString());
            PureModel pureModel = pureModelFunc.value();
            LambdaFunction<?> lambdaFunction = functionFunc.valueOf(pureModel);
            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
            MutableList<PlanGeneratorExtension> planGeneratorExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
            MutableList<PlanTransformer> planTransformers = planGeneratorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);
            Mapping pureMapping = pureModel.getMapping(mapping);
            Root_meta_core_runtime_Runtime pureRuntime = HelperRuntimeBuilder.buildPureRuntime(runtime, pureModel.getContext());
            Root_meta_pure_runtime_ExecutionContext executionContext = context == null ? new Root_meta_pure_runtime_ExecutionContext_Impl("") : HelperValueSpecificationBuilder.processExecutionContext(context, pureModel.getContext());
            MutableList<Root_meta_relational_testDataGeneration_TableRowIdentifiers> pureTableRowIdentifiers = Lists.mutable.empty();
            if (tableRowIdentifiers != null)
            {
                tableRowIdentifiers.forEach(x -> pureTableRowIdentifiers.add(HelperTestDataGenerationBuilder.processTestDataGenerationTableRowIdentifiers(x, pureModel.getContext())));
            }
            boolean pureHashStrings = hashStrings == null ? false : hashStrings;
            SingleExecutionPlan plan = PlanGenerator.transformExecutionPlan(generateTestDataPlan(lambdaFunction, pureMapping, pureRuntime, executionContext, pureTableRowIdentifiers, pureHashStrings, parameterNameValueMap, pureModel, identity), pureModel, clientVersion, identity, routerExtensions, planTransformers);
            Result result = planExecutor.execute(plan, planExecutor.buildDefaultExecutionState(plan, Maps.mutable.empty()).setRealizeAllocationResults(true), user, identity);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_TEST_DATA_STOP, System.currentTimeMillis() - start).toString());
            try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
            {
                return manageTestDataResult(identity, result, pureHashStrings, pureModel, LoggingEventType.GENERATE_TEST_DATA_ERROR);
            }
        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_TEST_DATA_ERROR, identity.getName());
        }
    }

    public static Response executeTestDataGenerateWithDefaultSeedUtil(Function<PureModel, LambdaFunction> functionFunc, Function0<PureModel> pureModelFunc, String mapping, Runtime runtime, ExecutionContext context, Boolean hashStrings, Map<String, Object> parameterNameValueMap, String clientVersion, Identity identity, String user, PlanExecutor planExecutor)
    {
        PureModel pureModel = pureModelFunc.value();
        Mapping pureMapping = pureModel.getMapping(mapping);
        Root_meta_core_runtime_Runtime pureRuntime = HelperRuntimeBuilder.buildPureRuntime(runtime, pureModel.getContext());
        Root_meta_pure_runtime_ExecutionContext executionContext = context == null ? new Root_meta_pure_runtime_ExecutionContext_Impl("") : HelperValueSpecificationBuilder.processExecutionContext(context, pureModel.getContext());
        return executeTestDataGenerateWithDefaultSeed(functionFunc, () -> pureModel, pureMapping, pureRuntime, executionContext, hashStrings, parameterNameValueMap, clientVersion, identity, user, planExecutor);
    }

    public static Response executeTestDataGenerateWithDefaultSeed(Function<PureModel, LambdaFunction> functionFunc, Function0<PureModel> pureModelFunc, Mapping pureMapping, Root_meta_core_runtime_Runtime pureRuntime, Root_meta_pure_runtime_ExecutionContext executionContext, Boolean hashStrings, Map<String, Object> parameterNameValueMap, String clientVersion, Identity identity, String user, PlanExecutor planExecutor)
    {
        try
        {
            long start = System.currentTimeMillis();
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_TEST_DATA_START, "").toString());
            PureModel pureModel = pureModelFunc.value();
            LambdaFunction<?> lambdaFunction = functionFunc.valueOf(pureModel);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_SEED_DATA_START, "").toString());

            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
            MutableList<PlanGeneratorExtension> planGeneratorExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
            MutableList<PlanTransformer> planTransformers = planGeneratorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);

            Root_meta_pure_executionPlan_ExecutionPlan seedPlan = SeedDataGeneration.generateSeedDataPlan(lambdaFunction, pureMapping, pureRuntime, executionContext, parameterNameValueMap, pureModel, routerExtensions, identity);
            Result seedDataResult = planExecutor.execute(PlanGenerator.transformExecutionPlan(seedPlan, pureModel, clientVersion, identity, routerExtensions, planTransformers), Maps.mutable.empty(), user, identity);
            MutableList<Root_meta_relational_testDataGeneration_TableRowIdentifiers> defaultTableRowIdentifiers = Lists.mutable.empty();
            if (seedDataResult instanceof ErrorResult)
            {
                String message = ((ErrorResult) seedDataResult).getMessage();
                LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_SEED_DATA_ERROR, message).toString());
                return manageTestDataResult(identity, seedDataResult, false, pureModel, LoggingEventType.GENERATE_TEST_DATA_ERROR);
            }
            else if (seedDataResult instanceof RelationalResult && ((RelationalResult) seedDataResult).getResultBuilder() instanceof RelationBuilder)
            {
                try
                {
                    RelationalResult relationalResult = (RelationalResult) seedDataResult;
                    RelationBuilder relationBuilder = (RelationBuilder) relationalResult.getResultBuilder();
                    TablePtr tablePtr = relationBuilder.getTablePointer();

                    TableRowIdentifiers tableRowIdentifier = new TableRowIdentifiers();
                    tableRowIdentifier.table = tablePtr;
                    tableRowIdentifier.rowIdentifiers = Lists.mutable.empty();
                    while (relationalResult.resultSet.next())
                    {
                        tableRowIdentifier.rowIdentifiers.add(SeedDataGeneration.getRowIdentifierFromNextRow(relationalResult));
                    }
                    defaultTableRowIdentifiers.add(HelperTestDataGenerationBuilder.processTestDataGenerationTableRowIdentifiers(tableRowIdentifier, pureModel.getContext()));
                }
                finally
                {
                    seedDataResult.close();
                }
            }
            else
            {
                if (seedDataResult instanceof RelationalResult)
                {
                    seedDataResult.close();
                }
                throw new RuntimeException("Unexpected Result :" + seedDataResult.getClass().getName());
            }
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_SEED_DATA_STOP, System.currentTimeMillis() - start).toString());
            boolean pureHashStrings = hashStrings == null ? false : hashStrings;
            Root_meta_pure_executionPlan_ExecutionPlan testDataPlan = generateTestDataPlan(lambdaFunction, pureMapping, pureRuntime, executionContext, defaultTableRowIdentifiers, pureHashStrings, parameterNameValueMap, pureModel, identity);
            SingleExecutionPlan plan = PlanGenerator.transformExecutionPlan(testDataPlan, pureModel, clientVersion, identity, routerExtensions, planTransformers);
            Result result = planExecutor.execute(plan, planExecutor.buildDefaultExecutionState(plan, Maps.mutable.empty()).setRealizeAllocationResults(true), user, identity);
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.GENERATE_TEST_DATA_STOP, System.currentTimeMillis() - start).toString());
            try (Scope scope = GlobalTracer.get().buildSpan("Manage Results").startActive(true))
            {
                return manageTestDataResult(identity, result, pureHashStrings, pureModel, LoggingEventType.GENERATE_TEST_DATA_ERROR);
            }

        }
        catch (Exception ex)
        {
            return ExceptionTool.exceptionManager(ex, LoggingEventType.GENERATE_TEST_DATA_ERROR, identity.getName());
        }
    }

    private static Root_meta_pure_executionPlan_ExecutionPlan generateTestDataPlan(LambdaFunction<?> lambdaFunction, Mapping mapping, Root_meta_core_runtime_Runtime runtime, Root_meta_pure_runtime_ExecutionContext executionContext, MutableList<Root_meta_relational_testDataGeneration_TableRowIdentifiers> tableRowIdentifiers, boolean hashStrings, Map<String, Object> parameterNameValueMap, PureModel pureModel, Identity identity)
    {
        Root_meta_pure_executionPlan_ExecutionPlan plan;
        try (Scope scope = GlobalTracer.get().buildSpan("Generate Plan").startActive(true))
        {
            RichIterable<? extends Root_meta_pure_extension_Extension> routerExtensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));
            MutableList<PlanGeneratorExtension> planGeneratorExtensions = org.eclipse.collections.api.factory.Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
            MutableList<PlanTransformer> planTransformers = planGeneratorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers);
            MutableList<org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<String, Root_meta_pure_functions_collection_List_Impl<Object>>> parameterNameValuePair = FastList.newList();
            for (Map.Entry<String, ?> e : parameterNameValueMap.entrySet())
            {
                Root_meta_pure_functions_collection_List_Impl<Object> lis = new Root_meta_pure_functions_collection_List_Impl<Object>("");
                lis._valuesAdd(e.getValue());
                parameterNameValuePair.add(newPair(e.getKey(), lis));
            }
            plan = core_relational_relational_testDataGeneration_testDataGeneration.Root_meta_relational_testDataGeneration_executionPlan_planTestDataGenerationWithParameterValuePairs_FunctionDefinition_1__Mapping_1__Runtime_1__ExecutionContext_1__TableRowIdentifiers_MANY__Boolean_1__TemporalMilestoningDates_$0_1$__Pair_MANY__Extension_MANY__ExecutionPlan_1_(
                    lambdaFunction,
                    mapping,
                    runtime,
                    executionContext,
                    tableRowIdentifiers,
                    hashStrings,
                    null,
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

    public static Response manageTestDataResult(Identity identity, Result result, boolean hashStrings, PureModel pureModel, LoggingEventType loggingEventType)
    {
        if (result instanceof ErrorResult)
        {
            String message = ((ErrorResult) result).getMessage();
            Result payload = ((ErrorResult) result).getPayload();

            if (payload != null)
            {
                RealizedRelationalResult realizedRelationalResult = (RealizedRelationalResult) payload;
                RelationBuilder relationBuilder = (RelationBuilder) realizedRelationalResult.getResultBuilder();
                StringBuilder stringBuilder = new StringBuilder(message);
                try
                {
                    stringBuilder.append("Example:\n");
                    TableRowIdentifiers tableRowIdentifiers = new TableRowIdentifiers();
                    tableRowIdentifiers.table = new TablePtr();
                    tableRowIdentifiers.table._type = "table";
                    tableRowIdentifiers.table.database = relationBuilder.database;
                    tableRowIdentifiers.table.schema = relationBuilder.schemaName;
                    tableRowIdentifiers.table.table = relationBuilder.relationName;
                    tableRowIdentifiers.rowIdentifiers = Lists.mutable.empty();

                    List<String> columns = realizedRelationalResult.columns.stream().map(SQLResultColumn::getQuotedLabelIfContainSpace).collect(Collectors.toList());
                    realizedRelationalResult.transformedRows.forEach(row ->
                    {
                        RowIdentifier rowIdentifier = new RowIdentifier();
                        rowIdentifier.columnValuePairs = Lists.mutable.empty();
                        int index = 0;
                        for (Object o : row)
                        {
                            ColumnValuePair cvPair = new ColumnValuePair();
                            cvPair.name = columns.get(index);
                            cvPair.value = o;
                            rowIdentifier.columnValuePairs.add(cvPair);
                            index += 1;
                        }
                        tableRowIdentifiers.rowIdentifiers.add(rowIdentifier);
                    });

                    stringBuilder.append(objectMapper.writeValueAsString(tableRowIdentifiers));
                    message = stringBuilder.toString();
                }
                catch (Exception ignored)
                {

                }
            }

            LOGGER.info(new LogInfo(identity.getName(), loggingEventType, message).toString());
            return Response.status(500).entity(message).build();
        }
        else if (result instanceof MultiResult)
        {
            MultiResult multiResult = (MultiResult) result;
            List<Result> subResults = new ArrayList<>(multiResult.getSubResults().values());
            List<ErrorResult> errorResults = subResults.stream().filter(x -> x instanceof ErrorResult).map(x -> (ErrorResult) x).collect(Collectors.toList());
            if (errorResults.size() > 0)
            {
                ErrorResult firstError = errorResults.get(0);
                String message = firstError.getMessage();
                LOGGER.info(new LogInfo(identity.getName(), loggingEventType, message).toString());
                return Response.status(500).entity(message).build();
            }

            String preDataString = multiResult.getSubResults().values().stream()
                    .map(x ->
                    {
                        RelationBuilder relationBuilder = (RelationBuilder) ((RealizedRelationalResult) x).getResultBuilder();
                        return Tuples.pair(relationBuilder, x);
                    })
                    .filter(x -> x.getOne().relationType == RelationType.TABLE)
                    .collect(Collectors.groupingBy(Pair::getOne))
                    .entrySet()
                    .stream()
                    .sorted(Comparator.comparing(x -> (x.getKey().schemaName + '.' + x.getKey().relationName)))
                    .map(x ->
                    {
                        String relationName = x.getKey().relationName;
                        String schemaName = x.getKey().schemaName;
                        String columnNames = ((RealizedRelationalResult) x.getValue().get(0).getTwo()).columns.stream().map(SQLResultColumn::getQuotedLabelIfContainSpace).collect(Collectors.joining(","));
                        String rows = x.getValue().stream()
                                .flatMap(y -> ((RealizedRelationalResult) y.getTwo()).transformedRows.stream())
                                .distinct()
                                .map(row ->
                                        row.stream()
                                                .map(val -> val == null ? "---null---" : (hashStrings ? core_relational_relational_helperFunctions_helperFunctions.Root_meta_relational_tests_csv_toCSVString_Any_1__String_1_(val instanceof String ? core_relational_relational_testDataGeneration_testDataGeneration.Root_meta_relational_testDataGeneration_hashString_String_1__String_1_((String) val, pureModel.getExecutionSupport()) : val, pureModel.getExecutionSupport()) : core_relational_relational_helperFunctions_helperFunctions.Root_meta_relational_tests_csv_toCSVString_Any_1__String_1_(val, pureModel.getExecutionSupport())))
                                                .collect(Collectors.joining(","))
                                )
                                .collect(Collectors.joining("\n", "", "\n"));
                        return schemaName + "\n" +
                                relationName + "\n" +
                                columnNames + "\n" +
                                rows;
                    })
                    .collect(Collectors.joining("-----\n"));

            String finalDataString = "".equals(preDataString) ? preDataString : preDataString + "-----\n";
            return Response.ok().entity(finalDataString).build();
        }
        else
        {
            throw new RuntimeException("Unexpected Result :" + result.getClass().getName());
        }
    }

    private static <T, U> org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.Pair<T, U> newPair(T first, U second)
    {
        return new Root_meta_pure_functions_collection_Pair_Impl<T, U>("")._first(first)._second(second);
    }
}