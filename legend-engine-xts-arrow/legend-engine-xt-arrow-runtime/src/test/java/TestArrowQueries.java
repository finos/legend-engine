/*
 * //  Copyright 2023 Goldman Sachs
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //       http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperRuntimeBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutorBuilder;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.api.model.ExecuteInput;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.generated.Root_meta_core_runtime_Runtime;
import org.finos.legend.pure.generated.core_pure_binding_extension;
import org.finos.legend.pure.generated.core_relational_relational_extensions_extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.pure.generated.core_external_format_arrow_contract.Root_meta_external_format_arrow_extension_arrowFormatExtension__Extension_1_;
import static org.finos.legend.pure.generated.core_relational_java_platform_binding_legendJavaPlatformBinding_relationalLegendJavaPlatformBindingExtension.Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionJavaPlatformBinding__Extension_1_;

public class TestArrowQueries
{


    @Test
    public void runTest()
    {
        try (
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
        )
        {

            ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
            ExecuteInput input = objectMapper.readValue(getClass().getClassLoader().getResource("arrowService.json"), ExecuteInput.class);

            PureModel model = org.finos.legend.engine.language.pure.compiler.Compiler.compile((PureModelContextData) input.model, DeploymentMode.TEST, IdentityFactoryProvider.getInstance().getAnonymousIdentity().getName());
            Root_meta_core_runtime_Runtime runtime = HelperRuntimeBuilder.buildPureRuntime(input.runtime, model.getContext());

            LambdaFunction<?> lambda = HelperValueSpecificationBuilder.buildLambda(input.function.body, input.function.parameters, model.getContext());
            MutableList<Root_meta_pure_extension_Extension> extensions = Lists.mutable.with(core_pure_binding_extension.Root_meta_external_format_shared_externalFormatExtension__Extension_1_(model.getExecutionSupport()));
            extensions.add(Root_meta_relational_executionPlan_platformBinding_legendJava_relationalExtensionJavaPlatformBinding__Extension_1_(model.getExecutionSupport()));
            extensions.add(core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtension__Extension_1_(model.getExecutionSupport()));
            extensions.add(Root_meta_external_format_arrow_extension_arrowFormatExtension__Extension_1_(model.getExecutionSupport()));
            SingleExecutionPlan plan = PlanGenerator.generateExecutionPlan(lambda, model.getMapping("relationalMapping::TradeAccountRelationalMapping"), runtime, null, model, "vX_X_X", PlanPlatform.JAVA, "test", extensions, LegendPlanTransformers.transformers);
            RelationalStoreExecutor relationalStoreExecutor = new RelationalStoreExecutorBuilder().build();
            PlanExecutor executor = PlanExecutor.newPlanExecutor(relationalStoreExecutor);
            PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs()
                    .withPlan(plan)
                    .build();
            StreamingResult streamingResult = (StreamingResult) executor.executeWithArgs(executeArgs);
            streamingResult.stream(baos, SerializationFormat.DEFAULT);
            assertAndValidateArrow(new ByteArrayInputStream(baos.toByteArray()), "expectedArrowServiceData.arrow");

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void assertAndValidateArrow(ByteArrayInputStream actualStream, String expectedArrowFilePath) throws Exception
    {
        try (
                BufferAllocator rootAllocator = new RootAllocator();
                ArrowStreamReader expectedArrowReader = new ArrowStreamReader(Files.newInputStream(Paths.get(this.getClass().getResource(expectedArrowFilePath).toURI())), rootAllocator);
                ArrowStreamReader actualArrowReader = new ArrowStreamReader(actualStream, rootAllocator)
        )
        {
            StringBuilder expected = new StringBuilder();
            StringBuilder actual = new StringBuilder();
            while (expectedArrowReader.loadNextBatch())
            {
                VectorSchemaRoot expectedVectorSchemaRoot = expectedArrowReader.getVectorSchemaRoot();
                expected.append(expectedVectorSchemaRoot.contentToTSVString());
            }
            while (actualArrowReader.loadNextBatch())
            {
                VectorSchemaRoot actualArrowReaderVectorSchemaRoot = actualArrowReader.getVectorSchemaRoot();
                actual.append(actualArrowReaderVectorSchemaRoot.contentToTSVString());
            }
            Assert.assertEquals(expected.toString(), actual.toString());
        }

    }

}

