// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.query.graphQL.extension.relational.directives;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.PlanTransformer;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.graphQL.metamodel.Directive;
import org.finos.legend.engine.protocol.graphQL.metamodel.Document;
import org.finos.legend.engine.protocol.pure.PureClientVersions;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.ExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.query.graphQL.api.execute.GraphQLExecute;
import org.finos.legend.engine.query.graphQL.api.execute.SerializedNamedPlans;
import org.finos.legend.engine.query.graphQL.api.execute.directives.IGraphQLDirectiveExtension;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.core_external_query_graphql_transformation_transformation_graphFetch;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TotalCountDirective implements IGraphQLDirectiveExtension
{
    @Override
    public ImmutableList<String> getSupportedDirectives()
    {
        return Lists.immutable.of("totalCount");
    }

    @Override
    public ExecutionPlan planDirective(Document document, PureModel pureModel, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> _class, Mapping mapping, Root_meta_core_runtime_Runtime runtime, RichIterable<? extends Root_meta_pure_extension_Extension> extensions, Iterable<? extends PlanTransformer> transformers)
    {
        try
        {
            org.finos.legend.pure.generated.Root_meta_external_query_graphQL_metamodel_sdl_Document queryDoc = GraphQLExecute.toPureModel(document, pureModel);
            RichIterable<? extends Root_meta_external_query_graphQL_transformation_queryToPure_NamedExecutionPlan> purePlans = core_external_query_graphql_transformation_transformation_graphFetch.Root_meta_external_query_graphQL_transformation_queryToPure_getPlanForTotalCountDirective_Class_1__Mapping_1__Runtime_1__Document_1__Extension_MANY__NamedExecutionPlan_MANY_(_class, mapping, runtime, queryDoc, extensions, pureModel.getExecutionSupport());
            List<SerializedNamedPlans> plans = purePlans.toList().stream().map(p ->
            {
                Root_meta_pure_executionPlan_ExecutionPlan nPlan = PlanPlatform.JAVA.bindPlan(p._plan(), "ID", pureModel, extensions);
                SerializedNamedPlans serializedPlans = new SerializedNamedPlans();
                serializedPlans.propertyName = p._name();
                serializedPlans.serializedPlan = PlanGenerator.stringToPlan(PlanGenerator.serializeToJSON(nPlan, PureClientVersions.production, pureModel, extensions, transformers));
                return serializedPlans;
            }).collect(Collectors.toList());
            if (plans.size() != 1)
            {
                throw new RuntimeException("Error computing plans for directive @totalCount - more than one execution plan");
            }
            return plans.get(0).serializedPlan;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object executeDirective(Directive directive, ExecutionPlan executionPlan, PlanExecutor planExecutor, Map<String, Result> parameterMap, Identity identity)
    {
        RelationalResult result = (RelationalResult) planExecutor.execute((SingleExecutionPlan) executionPlan, parameterMap, null, identity);
        RealizedRelationalResult realizedResult = ((RealizedRelationalResult) ((result).realizeInMemory()));
        List<List<Object>> resultSetRows = (realizedResult).resultSetRows;
        Long totalCount = (Long)((resultSetRows.get(0)).get(0));
        HashMap<String, Object> finalResult = new HashMap<>();
        finalResult.put("value", totalCount);
        if (parameterMap.containsKey("limit"))
        {
            finalResult.put("limit", ((ConstantResult)parameterMap.get("limit")).getValue());
        }
        if (parameterMap.containsKey("offset"))
        {
            finalResult.put("offset", ((ConstantResult)parameterMap.get("offset")).getValue());
        }
        return finalResult;
    }
}
