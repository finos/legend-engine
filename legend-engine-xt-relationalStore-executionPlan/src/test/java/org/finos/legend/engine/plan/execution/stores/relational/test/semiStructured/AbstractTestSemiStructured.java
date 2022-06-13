// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.semiStructured;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializer;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.core_external_format_json_extension;
import org.finos.legend.pure.generated.core_external_shared_extension;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_print;
import org.finos.legend.pure.generated.core_relational_relational_router_router_extension;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class AbstractTestSemiStructured
{
    private final PureModelContextData contextData;
    private final PureModel pureModel;
    private final PlanExecutor planExecutor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();

    public AbstractTestSemiStructured()
    {
        this.contextData = PureGrammarParser.newInstance().parseModel(readModelContentFromResource(this.modelResourcePath()));
        this.pureModel = Compiler.compile(contextData, null, null);
    }

    public abstract String modelResourcePath();

    protected String buildExecutionPlanString(String function, String mapping, String runtime)
    {
        MutableList<Root_meta_pure_router_extension_RouterExtension> extensions = getRouterExtensions();

        Function functionObject = Objects.requireNonNull(contextData.getElementsOfType(Function.class).stream().filter(x -> function.equals(x._package + "::" + x.name)).findFirst().orElse(null));

        Root_meta_pure_executionPlan_ExecutionPlan executionPlan = PlanGenerator.generateExecutionPlanAsPure(
                HelperValueSpecificationBuilder.buildLambda(functionObject.body, functionObject.parameters, pureModel.getContext()),
                pureModel.getMapping(mapping),
                pureModel.getRuntime(runtime),
                null,
                pureModel,
                PlanPlatform.JAVA,
                null,
                extensions
        );

        return core_pure_executionPlan_executionPlan_print.Root_meta_pure_executionPlan_toString_planToString_ExecutionPlan_1__Boolean_1__RouterExtension_MANY__String_1_(executionPlan, true, extensions, pureModel.getExecutionSupport());
    }

    protected String executeFunction(String function, String mapping, String runtime)
    {
        Function functionObject = Objects.requireNonNull(contextData.getElementsOfType(Function.class).stream().filter(x -> function.equals(x._package + "::" + x.name)).findFirst().orElse(null));

        SingleExecutionPlan executionPlan = PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(functionObject.body, functionObject.parameters, pureModel.getContext()),
                pureModel.getMapping(mapping),
                pureModel.getRuntime(runtime),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                getRouterExtensions(),
                LegendPlanTransformers.transformers
        );

        RelationalResult result = (RelationalResult) this.planExecutor.execute(executionPlan);
        return new String(new RelationalResultToCSVSerializer(result).flush().toByteArray(), StandardCharsets.UTF_8);
    }

    private MutableList<Root_meta_pure_router_extension_RouterExtension> getRouterExtensions()
    {
        MutableList<Root_meta_pure_router_extension_RouterExtension> extensions = Lists.mutable.empty();
        extensions.addAll(Lists.mutable.withAll(core_relational_relational_router_router_extension.Root_meta_pure_router_extension_defaultRelationalExtensions__RouterExtension_MANY_(pureModel.getExecutionSupport())));
        extensions.addAll(core_external_shared_extension.Root_meta_external_shared_format_routerExtensions_String_1__ExternalFormatExtension_MANY__RouterExtension_MANY_("externalFormat", Lists.mutable.with(core_external_format_json_extension.Root_meta_external_format_json_jsonFormatExtension__ExternalFormatExtension_1_(pureModel.getExecutionSupport())), pureModel.getExecutionSupport()).toList());
        return extensions;
    }

    private String readModelContentFromResource(String resourcePath)
    {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(Objects.requireNonNull(AbstractTestSemiStructured.class.getResourceAsStream(resourcePath)))))
        {
            return buffer.lines().collect(Collectors.joining("\n"));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
