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
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.core_external_format_json_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.generated.core_pure_binding_extension;
import org.finos.legend.pure.generated.core_pure_executionPlan_executionPlan_print;
import org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension;
import org.finos.legend.pure.generated.core_relational_relational_lineage_scanColumns_scanColumns;

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
        MutableList<Root_meta_pure_extension_Extension> extensions = getExtensions();

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

        return core_pure_executionPlan_executionPlan_print.Root_meta_pure_executionPlan_toString_planToString_ExecutionPlan_1__Boolean_1__Extension_MANY__String_1_(executionPlan, true, extensions, pureModel.getExecutionSupport());
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
                getExtensions(),
                LegendPlanTransformers.transformers
        );

        RelationalResult result = (RelationalResult) this.planExecutor.execute(executionPlan);
        return new String(new RelationalResultToCSVSerializer(result).flush().toByteArray(), StandardCharsets.UTF_8);
    }

    protected String scanColumns(String function, String mapping)
    {
        Function functionObject = Objects.requireNonNull(contextData.getElementsOfType(Function.class).stream().filter(x -> function.equals(x._package + "::" + x.name)).findFirst().orElse(null));
        return core_relational_relational_lineage_scanColumns_scanColumns.Root_meta_pure_lineage_scanColumns_scanColumnsAndReturnString_ValueSpecification_1__Mapping_1__String_1_(HelperValueSpecificationBuilder.buildLambda(functionObject.body, functionObject.parameters, pureModel.getContext())._expressionSequence().getOnly(), pureModel.getMapping(mapping), pureModel.getExecutionSupport());
    }

    private MutableList<Root_meta_pure_extension_Extension> getExtensions()
    {
        MutableList<Root_meta_pure_extension_Extension> extensions = Lists.mutable.empty();
        extensions.addAllIterable(
                core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionsWithLegendJavaPlatformBinding_ExternalFormatLegendJavaPlatformBindingDescriptor_MANY__Extension_MANY_(
                        Lists.mutable.with(
                                core_external_format_json_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_json_executionPlan_platformBinding_legendJava_jsonSchemaJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(pureModel.getExecutionSupport())
                        ),
                        pureModel.getExecutionSupport()
                )
        );
        extensions.add(core_pure_binding_extension.Root_meta_external_shared_format_externalFormatExtension__Extension_1_(pureModel.getExecutionSupport()));
        extensions.add(core_external_format_json_externalFormatContract.Root_meta_external_format_json_extension_jsonSchemaFormatExtension__Extension_1_(pureModel.getExecutionSupport()));

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

    protected String wrapPreAndFinallyExecutionSqlQuery(String TDSType, String expectedRelational)
    {
        return  "RelationalBlockExecutionNode\n" +
                "(\n" +
                TDSType +
                "  (\n" +
                "    SQL\n" +
                "    (\n" +
                "      type = Void\n" +
                "      resultColumns = []\n" +
                "      sql = ALTER SESSION SET QUERY_TAG = '{\"executionTraceID\" : \"${execID}\", \"engineUser\" : \"${userId}\", \"referer\" : \"${referer}\"}';\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n" +
                expectedRelational +
                "  ) \n" +
                "  finallyExecutionNodes = \n" +
                "  (\n" +
                "    SQL\n" +
                "    (\n" +
                "      type = Void\n" +
                "      resultColumns = []\n" +
                "      sql = ALTER SESSION UNSET QUERY_TAG;\n" +
                "      connection = RelationalDatabaseConnection(type = \"Snowflake\")\n" +
                "    )\n" +
                "  )\n" +
                ")\n";
    }

}
