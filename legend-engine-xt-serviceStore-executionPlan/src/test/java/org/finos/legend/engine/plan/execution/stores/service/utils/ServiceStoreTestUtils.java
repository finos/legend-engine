// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.service.utils;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStoreExecutorBuilder;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.generated.core_external_format_json_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.generated.core_pure_binding_extension;
import org.finos.legend.pure.generated.core_servicestore_java_platform_binding_legendJavaPlatformBinding_serviceStoreLegendJavaPlatformBindingExtension;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.finos.legend.server.pac4j.kerberos.LocalCredentials;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceStoreTestUtils
{
    private static PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(new ServiceStoreExecutorBuilder().build(), InMemory.build());

    public static String readGrammarFromPureFile(String path)
    {
        String pureGrammar;
        try
        {
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(ServiceStoreTestUtils.class.getResourceAsStream(path))))
            {
                pureGrammar = buffer.lines().collect(Collectors.joining("\n"));
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return pureGrammar;
    }

    public static SingleExecutionPlan buildPlanForQuery(String grammar)
    {
        return buildPlanForQuery(grammar, "meta::external::store::service::showcase::mapping::ServiceStoreMapping", "meta::external::store::service::showcase::runtime::ServiceStoreRuntime");
    }

    public static SingleExecutionPlan buildPlanForQuery(String grammar, String mapping, String runtime)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(grammar);
        PureModel pureModel = Compiler.compile(contextData, null, null);

        MutableList<Root_meta_pure_extension_Extension> extensions = Lists.mutable.empty();
        extensions.addAllIterable(
                core_servicestore_java_platform_binding_legendJavaPlatformBinding_serviceStoreLegendJavaPlatformBindingExtension.Root_meta_external_store_service_executionPlan_platformBinding_legendJava_serviceStoreExtensionsWithLegendJavaPlatformBinding_ExternalFormatLegendJavaPlatformBindingDescriptor_MANY__Extension_MANY_(
                        Lists.mutable.with(
                                core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_flatdata_executionPlan_platformBinding_legendJava_flatDataJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(pureModel.getExecutionSupport()),
                                core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_json_executionPlan_platformBinding_legendJava_jsonSchemaJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(pureModel.getExecutionSupport())
                        ),
                        pureModel.getExecutionSupport()
                )
        );
        extensions.add(core_pure_binding_extension.Root_meta_external_shared_format_externalFormatExtension__Extension_1_(pureModel.getExecutionSupport()));
        extensions.add(core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_(pureModel.getExecutionSupport()));
        extensions.add(core_external_format_json_externalFormatContract.Root_meta_external_format_json_extension_jsonSchemaFormatExtension__Extension_1_(pureModel.getExecutionSupport()));

        Function queryFunctionExpressions = ListIterate.detect(contextData.getElementsOfType(Function.class), f -> "query__Any_1_".equals(f.name));

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(((Lambda) queryFunctionExpressions.body.get(0)).body, ((Lambda) queryFunctionExpressions.body.get(0)).parameters, pureModel.getContext()),
                pureModel.getMapping(mapping),
                pureModel.getRuntime(runtime),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                extensions,
                LegendPlanTransformers.transformers
        );
    }

    public static String executePlan(SingleExecutionPlan plan)
    {
        return executePlan(plan, Maps.mutable.empty());
    }

    public static String executePlan(SingleExecutionPlan plan, Map<String, ?> params)
    {
        SingleExecutionPlan singleExecutionPlan = plan.getSingleExecutionPlan(params);

        Map<String, Result> vars = org.eclipse.collections.impl.factory.Maps.mutable.ofInitialCapacity(params.size());
        params.forEach((key, value) -> vars.put(key, new ConstantResult(value)));

        JsonStreamingResult result = (JsonStreamingResult) planExecutor.execute(singleExecutionPlan, vars, (String) null, Lists.mutable.with(new KerberosProfile(LocalCredentials.INSTANCE)), null);
        return result.flush(new JsonStreamToJsonDefaultSerializer(result));
    }
}
