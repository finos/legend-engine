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
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.engine.external.shared.format.model.ExternalFormatPlanGenerationExtensionLoader;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_ExternalFormatExtension;
import org.finos.legend.pure.generated.Root_meta_pure_router_extension_RouterExtension;
import org.finos.legend.pure.generated.Root_meta_pure_runtime_ExecutionContext_Impl;
import org.finos.legend.pure.generated.core_external_shared_extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.ExecutionContext;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Runtime;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;

public abstract class TestExternalFormatQueries
{
    protected String runTest(String grammar, String query, String mappingPath, String runtimePath, String input)
    {
        try
        {
            return runTest(grammar, query, mappingPath, runtimePath, new ByteArrayInputStream(input.getBytes("UTF-8")));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected String runTest(PureModelContextData generated, String grammar, String query, String mappingPath, String runtimePath, String input)
    {
        try
        {
            return runTest(generated, grammar, query, mappingPath, runtimePath, new ByteArrayInputStream(input.getBytes("UTF-8")));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected String runTest(String grammar, String query, String mappingPath, String runtimePath, InputStream input)
    {
        return runTest(null, grammar, query, mappingPath, runtimePath, input);
    }

    protected String runTest(PureModelContextData generated, String grammar, String query, String mappingPath, String runtimePath, InputStream input)
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
            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(lambdaProtocol.body, Lists.fixedSize.<Variable>empty(), model.getContext());

            ExecutionContext context = new Root_meta_pure_runtime_ExecutionContext_Impl(" ")._enableConstraints(true);

            RichIterable<Root_meta_external_shared_format_ExternalFormatExtension> planGenerationExtensions = LazyIterate.collect(ExternalFormatPlanGenerationExtensionLoader.extensions().values(), ext -> ext.getPureExtension(model.getExecutionSupport()));
            RichIterable<? extends Root_meta_pure_router_extension_RouterExtension> extensions = core_external_shared_extension.Root_meta_external_shared_format_routerExtensions_String_1__ExternalFormatExtension_MANY__RouterExtension_MANY_("externalFormat", planGenerationExtensions, model.getExecutionSupport());

            Mapping mapping = model.getMapping(mappingPath);
            Runtime runtime = model.getRuntime(runtimePath);
            String plan = PlanGenerator.generateExecutionPlanAsString(lambda, mapping, runtime, context, model, "vX_X_X", PlanPlatform.JAVA, "test", extensions, LegendPlanTransformers.transformers);

            PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(true);
            Result result = executor.execute(plan, input);
            StreamingResult streamingResult = (StreamingResult) result;
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

    protected String firmSelfMapping()
    {
        return "###Mapping\n" +
                "\n" +
                "Mapping test::firm::mapping::SelfMapping\n" +
                "(\n" +
                "   test::firm::model::Firm: Pure\n" +
                "   {\n" +
                "      ~src test::firm::model::Firm\n" +
                "      \n" +
                "      name      : $src.name,\n" +
                "      ranking   : $src.ranking,\n" +
                "      addresses : $src.addresses,\n" +
                "      employees : $src.employees      \n" +
                "   }\n" +
                "\n" +
                "   test::firm::model::Person: Pure\n" +
                "   {\n" +
                "      ~src test::firm::model::Person\n" +
                "      \n" +
                "      firstName      : $src.firstName,\n" +
                "      lastName       : $src.lastName,\n" +
                "      dateOfBirth    : $src.dateOfBirth,\n" +
                "      isAlive        : $src.isAlive,\n" +
                "      heightInMeters : $src.heightInMeters,\n" +
                "      addresses      : $src.addresses\n" +
                "   }\n" +
                "   \n" +
                "   test::firm::model::AddressUse: Pure\n" +
                "   {\n" +
                "      ~src test::firm::model::AddressUse\n" +
                "      \n" +
                "      addressType : $src.addressType,\n" +
                "      address     : $src.address\n" +
                "   }\n" +
                "\n" +
                "   test::firm::model::Address: Pure\n" +
                "   {\n" +
                "      ~src test::firm::model::Address\n" +
                "      \n" +
                "      firstLine  : $src.firstLine,\n" +
                "      secondLine : $src.secondLine,\n" +
                "      city       : $src.city,\n" +
                "      region     : $src.region,\n" +
                "      country    : $src.country,\n" +
                "      position   : $src.position\n" +
                "   }  \n" +
                "   \n" +
                "   test::firm::model::GeographicPosition: Pure\n" +
                "   {\n" +
                "      ~src test::firm::model::GeographicPosition\n" +
                "      \n" +
                "      latitude  : $src.latitude,\n" +
                "      longitude : $src.longitude\n" +
                "   }\n" +
                ")\n";
    }

    protected String urlRuntime(String mapping, String rootClass, String contentType)
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
                "    ModelStore:\n" +
                "    [\n" +
                "      c1:\n" +
                "      #{\n" +
                "        UrlConnection\n" +
                "        {\n" +
                "          class: " + rootClass + ";\n" +
                "          url: 'executor:default';\n" +
                "          contentType: '" + contentType + "';\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
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

    protected InputStream resource(String name)
    {
        return Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(name), "Failed to find resource " + name);
    }

    protected Reader resourceReader(String name)
    {
        return new InputStreamReader(resource(name));
    }
}
