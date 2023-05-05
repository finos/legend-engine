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

package org.finos.legend.engine.external.format.flatdata.read.test;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.external.format.flatdata.transformation.fromModel.ModelToFlatDataConfiguration;
import org.finos.legend.engine.external.shared.format.model.transformation.fromModel.ModelToSchemaGenerationTest;
import org.finos.legend.engine.external.shared.runtime.test.TestExternalFormatQueries;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.ModelUnit;
import org.finos.legend.pure.generated.core_external_format_flatdata_externalFormatContract;
import org.finos.legend.pure.generated.core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;

public class TestFlatDataQueries extends TestExternalFormatQueries
{
    @BeforeClass
    public static void setup()
    {
        ExecutionSupport executionSupport = Compiler.compile(PureModelContextData.newPureModelContextData(), null, null).getExecutionSupport();
        formatExtensions = Collections.singletonList(core_external_format_flatdata_externalFormatContract.Root_meta_external_format_flatdata_extension_flatDataFormatExtension__Extension_1_(executionSupport));
        formatDescriptors = Collections.singletonList(core_external_format_flatdata_java_platform_binding_legendJavaPlatformBinding_descriptor.Root_meta_external_format_flatdata_executionPlan_platformBinding_legendJava_flatDataJavaBindingDescriptor__ExternalFormatLegendJavaPlatformBindingDescriptor_1_(executionSupport));
    }

    @Test
    public void testInternalizeWithGraphFetchAndDefects()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{longitude}}#"; // latitude property skipped on purpose to test graphFetch expands tree scope to include constraint on latitude

        try
        {
            runTest(generated,
                    "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetch(" + positionTree + ")->serialize(" + positionTree + ")",
                    Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
            Assert.fail("Expected exception to be raised. Not found any");
        }
        catch (Exception e)
        {
            Assert.assertEquals("java.lang.IllegalStateException: Constraint :[validLatitude] violated in the Class GeographicPosition", e.getMessage());
        }
    }

    @Test
    public void testInternalizeWithGraphFetchUnexpandedAndDefects()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::GeographicPosition");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        String positionTree = "#{test::firm::model::GeographicPosition{latitude, longitude}}#";

        try
        {
            runTest(generated,
                    "data:ByteStream[1]|test::firm::model::GeographicPosition->internalize(test::gen::TestBinding, $data)->graphFetchUnexpanded(" + positionTree + ")->serialize(" + positionTree + ")",
                    Maps.mutable.with("data", resource("queries/positionWithExactHeadings.csv")));
            Assert.fail("Expected exception to be raised. Not found any");
        }
        catch (Exception e)
        {
            Assert.assertEquals("java.lang.IllegalStateException: Constraint :[validLatitude] violated in the Class GeographicPosition", e.getMessage());
        }
    }

    @Test
    public void testExternalizeWithCheckedTree()
    {
        String modelGrammar = firmModel();
        ModelUnit modelUnit = new ModelUnit();
        modelUnit.packageableElementIncludes = Collections.singletonList("test::firm::model::Person");
        PureModelContextData generated = ModelToSchemaGenerationTest.generateSchema(modelGrammar, modelUnit, toFlatDataConfig(), true, "test::gen::TestBinding");

        try
        {
            runTest(generated,
                    "data:ByteStream[1]|test::firm::model::Person->internalize(test::gen::TestBinding, $data)->graphFetchChecked(" + personTree() + ")->externalize(test::gen::TestBinding, checked(" + personTree() + ", test::gen::TestBinding))",
                    Maps.mutable.with("data", resource("queries/peopleWithExactHeadings.csv")));
            Assert.fail("Exception expected");
        }
        catch (Exception e)
        {
            Assert.assertEquals("Assert failure at (resource:/core_external_format_flatdata_java_platform_binding/legendJavaPlatformBinding/externalize.pure line:95 column:3), \"Multi Section serialization is not yet supported !!\"", e.getMessage());
        }
    }

    private ModelToFlatDataConfiguration toFlatDataConfig()
    {
        ModelToFlatDataConfiguration config = new ModelToFlatDataConfiguration();
        config.targetSchemaSet = "test::gen::TestSchemaSet";
        config.format = "FlatData";
        return config;
    }
}
