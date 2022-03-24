// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.external.format.protobuf.tests;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.external.format.protobuf.schema.generations.ProtobufGenerationConfig;
import org.finos.legend.engine.external.format.protobuf.extension.ProtobufGenerationConfigFromFileGenerationSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.fileGeneration.FileGenerationSpecification;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.pure.generated.Root_meta_external_format_protobuf_generation_ProtobufConfig;
import org.finos.legend.pure.generated.Root_meta_pure_generation_metamodel_GenerationOutput;
import org.finos.legend.pure.generated.core_external_format_protobuf_transformation_pureToProtocolBuffers;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

public class TestProtobufFileGeneration
{
    @Test
    public void testSimpleProtobuf()
    {
        try
        {
            PureModelContextData pureModelContextData = getProtocol("simpleFileGeneration.json");
            PureModel pureModel = new PureModel(pureModelContextData, null, DeploymentMode.TEST);
            FileGenerationSpecification fileGeneration = pureModelContextData.getElementsOfType(FileGenerationSpecification.class).get(0);
            ProtobufGenerationConfig protobufConfig = ProtobufGenerationConfigFromFileGenerationSpecificationBuilder.build(fileGeneration);
            Root_meta_external_format_protobuf_generation_ProtobufConfig metaModelConfig = protobufConfig.transformToPure(pureModel);
            List<? extends Root_meta_pure_generation_metamodel_GenerationOutput> outputs = core_external_format_protobuf_transformation_pureToProtocolBuffers.Root_meta_external_format_protobuf_generation_generateProtobufFromPureWithScope_ProtobufConfig_1__ProtobufOutput_MANY_(metaModelConfig, pureModel.getExecutionSupport()).toList();
            Assert.assertEquals(outputs.size(), 2);
            Assert.assertEquals("_other.proto", outputs.get(0)._fileName());
            Assert.assertEquals("syntax = \"proto3\";\n" +
                                        "package _other;\n" +
                                        "\n" +
                                        "message OtherClass {\n" +
                                        "  string stuff = 1;\n" +
                                        "}", outputs.get(0)._content());
            Assert.assertEquals("_meta_pure_generation_tests_model.proto", outputs.get(1)._fileName());
            Assert.assertEquals("syntax = \"proto3\";\n" +
                                        "package _meta.pure.generation.tests.model;\n" +
                                        "\n" +
                                        "message Address {\n" +
                                        "  string street = 1;\n" +
                                        "}\n" +
                                        "\n" +
                                        "message Firm {\n" +
                                        "  string legal_name = 1;\n" +
                                        "  repeated Person employees = 2;\n" +
                                        "  repeated Address addresses = 3;\n" +
                                        "  int64 count = 4;\n" +
                                        "}\n" +
                                        "\n" +
                                        "message Person {\n" +
                                        "  string first_name = 1;\n" +
                                        "  string last_name = 2;\n" +
                                        "  repeated Address addresses = 3;\n" +
                                        "  Firm firm = 4;\n" +
                                        "}", outputs.get(1)._content());
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private PureModelContextData getProtocol(String fileName) throws JsonProcessingException
    {
        String jsonString = this.getResourceAsString(fileName);
        return ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports().readValue(jsonString, PureModelContextData.class);
    }

    private String getResourceAsString(String fileName)
    {
        InputStream inputStream = TestProtobufFileGeneration.class.getResourceAsStream(fileName);
        Scanner scanner = new Scanner(inputStream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
