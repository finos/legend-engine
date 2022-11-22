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

package org.finos.legend.engine.external.shared.runtime.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.url.StreamProvider;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_executionPlan_engine_java_ExternalFormatLegendJavaPlatformBindingDescriptor;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext_Impl;
import org.finos.legend.pure.generated.core_java_platform_binding_legendJavaPlatformBinding_binding_bindingLegendJavaPlatformBindingExtension;
import org.finos.legend.pure.generated.core_pure_binding_extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class TestExternalFormatQueries
{
    protected static List<Root_meta_pure_extension_Extension> formatExtensions;
    protected static List<Root_meta_external_shared_format_executionPlan_engine_java_ExternalFormatLegendJavaPlatformBindingDescriptor> formatDescriptors = Lists.mutable.empty();

    protected String runTest(PureModelContextData modelData, String query)
    {
        return runTest(modelData, query, Maps.mutable.empty());
    }

    protected String runTest(PureModelContextData modelData, String query, StreamProvider streamProvider)
    {
        return runTest(modelData, query, Maps.mutable.empty(), streamProvider);
    }

    protected String runTest(PureModelContextData modelData, String query, Map<String, ?> params)
    {
        return runTest(modelData, query, params, null);
    }

    protected String runTest(PureModelContextData modelData, String query, Map<String, ?> params, StreamProvider streamProvider)
    {
        try
        {
            PureModel model = Compiler.compile(modelData, DeploymentMode.TEST, null);

            PureGrammarParser parser = PureGrammarParser.newInstance();
            Lambda lambdaProtocol = parser.parseLambda(query);
            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(lambdaProtocol.body, lambdaProtocol.parameters, model.getContext());

            MutableList<Root_meta_pure_extension_Extension> extensions = Lists.mutable.with(core_pure_binding_extension.Root_meta_external_shared_format_externalFormatExtension__Extension_1_(model.getExecutionSupport()));
            extensions.addAll(formatExtensions);
            extensions.addAllIterable(core_java_platform_binding_legendJavaPlatformBinding_binding_bindingLegendJavaPlatformBindingExtension.Root_meta_external_shared_format_executionPlan_platformBinding_legendJava_bindingExtensionsWithLegendJavaPlatformBinding_ExternalFormatLegendJavaPlatformBindingDescriptor_MANY__Extension_MANY_(Lists.mutable.withAll(formatDescriptors), model.getExecutionSupport()));

            SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(lambda, null, null, null, model, "vX_X_X", PlanPlatform.JAVA, "test", extensions, LegendPlanTransformers.transformers);
            PlanExecutor executor = PlanExecutor.newPlanExecutor();
            PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs()
                    .withPlan(plan)
                    .withParams(params)
                    .withInputAsStreamProvider(streamProvider)
                    .build();
            StreamingResult streamingResult = (StreamingResult) executor.executeWithArgs(executeArgs);
            return streamingResult.flush(streamingResult.getSerializer(SerializationFormat.DEFAULT));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected String firmModel()
    {
        return "###Pure\n" +
                "Enum test::firm::model::AddressType\n" +
                "{\n" +
                "   Headquarters,\n" +
                "   RegionalOffice,\n" +
                "   Home,\n" +
                "   Holiday\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Firm\n" +
                "{\n" +
                "   name      : String[1];\n" +
                "   ranking   : Integer[0..1];\n" +
                "   addresses : test::firm::model::AddressUse[1..*];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Address\n" +
                "{\n" +
                "   firstLine  : String[1];\n" +
                "   secondLine : String[0..1];\n" +
                "   city       : String[0..1];\n" +
                "   region     : String[0..1];\n" +
                "   country    : String[1];\n" +
                "   position   : test::firm::model::GeographicPosition[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::GeographicPosition\n" +
                "[\n" +
                "   validLatitude: ($this.latitude >= -90) && ($this.latitude <= 90),\n" +
                "   validLongitude: ($this.longitude >= -180) && ($this.longitude <= 180)\n" +
                "]\n" +
                "{\n" +
                "   latitude  : Decimal[1];\n" +
                "   longitude : Decimal[1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::AddressUse\n" +
                "{\n" +
                "   addressType : test::firm::model::AddressType[1];\n" +
                "   address     : test::firm::model::Address[1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Person\n" +
                "{\n" +
                "   firstName      : String[1];\n" +
                "   lastName       : String[1];\n" +
                "   dateOfBirth    : StrictDate[0..1];   \n" +
                "   addresses      : test::firm::model::AddressUse[*];\n" +
                "   isAlive        : Boolean[1];\n" +
                "   heightInMeters : Float[1];\n" +
                "}\n" +
                "\n" +
                "Association test::firm::model::Firm_Person\n" +
                "{\n" +
                "   firm      : test::firm::model::Firm[1];\n" +
                "   employees : test::firm::model::Person[*];\n" +
                "}\n";
    }

    protected String urlStreamRuntime(String mapping, String binding)
    {
        return "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    " + mapping + "\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    " + binding + ":\n" +
                "    [\n" +
                "      c1:\n" +
                "      #{\n" +
                "        ExternalFormatConnection\n" +
                "        {\n" +
                "          source: UrlStream\n" +
                "          {\n" +
                "            url: 'executor:default';\n" +
                "          };\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

    }

    protected String firmTree()
    {
        return "#{test::firm::model::Firm {name, ranking}}#";
    }

    protected String personTree()
    {
        return "#{test::firm::model::Person {firstName, lastName, dateOfBirth, isAlive, heightInMeters}}#";
    }

    protected String fullTree()
    {
        return "#{test::firm::model::Firm {" +
                "  name, " +
                "  ranking," +
                "  employees {" +
                "    firstName," +
                "    lastName," +
                "    dateOfBirth,\n" +
                "    isAlive," +
                "    heightInMeters," +
                "    addresses {" +
                "      addressType," +
                "      address {" +
                "        firstLine," +
                "        secondLine," +
                "        city," +
                "        region," +
                "        country," +
                "        position {" +
                "          latitude," +
                "          longitude" +
                "        }" +
                "      }" +
                "    }" +
                "  }," +
                "  addresses {" +
                "    addressType," +
                "    address {" +
                "      firstLine," +
                "      secondLine," +
                "      city," +
                "      region," +
                "      country," +
                "      position {" +
                "        latitude," +
                "        longitude" +
                "      }" +
                "    }" +
                "  }" +
                "}}#";
    }

    protected Reader resourceReader(String name)
    {
        return new InputStreamReader(resource(name));
    }

    protected String resourceAsString(String path)
    {
        byte[] bytes;
        try
        {
            bytes = Files.readAllBytes(Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(path), "Failed to get resource " + path).toURI()));
        }
        catch (IOException | URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        String string = new String(bytes, StandardCharsets.UTF_8);
        return string.replaceAll("\\R", "\n");
    }

    protected InputStream resource(String name)
    {
        return Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(name), "Failed to find resource " + name);
    }

    // TODO: to be removed
    protected String runTest(PureModelContextData generated, String grammar, String query, String mappingPath, String runtimePath, InputStream input, Map<String, ?> params, List<Root_meta_pure_extension_Extension> formatExtensions)
    {
        try
        {
            PureModelContextData parsed = PureGrammarParser.newInstance().parseModel(grammar);
            PureModelContextData modelData = generated == null ? parsed : parsed.combine(generated);
            ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
            String json = objectMapper.writeValueAsString(modelData);
            modelData = objectMapper.readValue(json, PureModelContextData.class);
            PureModel model = Compiler.compile(modelData, DeploymentMode.TEST, null);

            PureGrammarParser parser = PureGrammarParser.newInstance();
            Lambda lambdaProtocol = parser.parseLambda(query);
            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(lambdaProtocol.body, lambdaProtocol.parameters, model.getContext());

            ExecutionContext context = new Root_meta_pure_runtime_ExecutionContext_Impl(" ")._enableConstraints(true);

            MutableList<Root_meta_pure_extension_Extension> extensions = Lists.mutable.with(core_pure_binding_extension.Root_meta_external_shared_format_externalFormatExtension__Extension_1_(model.getExecutionSupport()));
            extensions.addAll(formatExtensions);
            extensions.addAllIterable(core_java_platform_binding_legendJavaPlatformBinding_binding_bindingLegendJavaPlatformBindingExtension.Root_meta_external_shared_format_executionPlan_platformBinding_legendJava_bindingExtensionsWithLegendJavaPlatformBinding_ExternalFormatLegendJavaPlatformBindingDescriptor_MANY__Extension_MANY_(Lists.mutable.withAll(formatDescriptors), model.getExecutionSupport()));

            Mapping mapping = model.getMapping(mappingPath);
            Runtime runtime = model.getRuntime(runtimePath);
            String plan = PlanGenerator.generateExecutionPlanAsString(lambda, mapping, runtime, context, model, "vX_X_X", PlanPlatform.JAVA, "test", extensions, LegendPlanTransformers.transformers);
            PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(true);
            StreamingResult streamingResult = (StreamingResult) executor.execute(plan, input);
            return streamingResult.flush(streamingResult.getSerializer(SerializationFormat.DEFAULT));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
