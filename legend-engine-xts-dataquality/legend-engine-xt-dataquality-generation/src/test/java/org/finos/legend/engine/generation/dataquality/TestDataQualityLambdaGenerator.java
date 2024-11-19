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

package org.finos.legend.engine.generation.dataquality;

import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Option;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.exception.PureAssertFailException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

//NOTE: conversion tests can be found in the pure-test module. Tests here are to ensure the java->pure flow e2e.
public class TestDataQualityLambdaGenerator
{
    @Test
    public void testNestedConstraints()
    {
        String validation = "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::Validation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::external::dataquality::tests::domain::dataqualitymappings, meta::external::dataquality::tests::domain::DataQualityRuntime);\n" +
                "    validationTree: $[\n" +
                "      meta::external::dataquality::tests::domain::Person<mustBeOfLegalAge>{\n" +
                "        name,\n" +
                "        addresses<validAddressId>{\n" +
                "         addressId\n" +
                "        }\n" +
                "      }\n" +
                "    ]$;\n" +
                "}";

        assertLambdaException(validation, "Nested constraints are not currently supported!");
    }

    @Test
    public void testLambdaGeneration()
    {
        String validation = "###DataQualityValidation\n" +
                "DataQualityValidation meta::dataquality::Validation\n" +
                "{\n" +
                "    context: fromMappingAndRuntime(meta::external::dataquality::tests::domain::dataqualitymappings, meta::external::dataquality::tests::domain::DataQualityRuntime);\n" +
                "    validationTree: $[\n" +
                "      meta::external::dataquality::tests::domain::Person<mustBeOfLegalAge>{\n" +
                "        name" +
                "      }\n" +
                "    ]$;\n" +
                "}";

        String expected = "{\"_type\":\"lambda\",\"body\":[{\"fControl\":\"from_T_m__Mapping_1__Runtime_1__T_m_\",\"function\":\"from\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"serialize_Checked_MANY__RootGraphFetchTree_1__String_1_\",\"function\":\"serialize\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"graphFetchChecked_T_MANY__RootGraphFetchTree_1__Checked_MANY_\",\"function\":\"graphFetchChecked\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"filter_T_MANY__Function_1__T_MANY_\",\"function\":\"filter\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"getAll_Class_1__T_MANY_\",\"function\":\"getAll\",\"_type\":\"func\",\"parameters\":[{\"fullPath\":\"meta::external::dataquality::tests::domain::Person\",\"_type\":\"packageableElementPtr\"}]},{\"_type\":\"lambda\",\"body\":[{\"fControl\":\"not_Boolean_1__Boolean_1_\",\"function\":\"not\",\"_type\":\"func\",\"parameters\":[{\"fControl\":\"greaterThanEqual_Number_1__Number_1__Boolean_1_\",\"function\":\"greaterThanEqual\",\"_type\":\"func\",\"parameters\":[{\"_type\":\"property\",\"property\":\"age\",\"parameters\":[{\"_type\":\"var\",\"name\":\"x\"}]},{\"_type\":\"integer\",\"value\":18}]}]}],\"parameters\":[{\"multiplicity\":{\"upperBound\":1,\"lowerBound\":1},\"_type\":\"var\",\"name\":\"x\",\"class\":\"meta::external::dataquality::tests::domain::Person\"}]}]},{\"_type\":\"classInstance\",\"type\":\"rootGraphFetchTree\",\"value\":{\"subTrees\":[{\"_type\":\"propertyGraphFetchTree\",\"property\":\"name\"},{\"_type\":\"propertyGraphFetchTree\",\"property\":\"age\"}],\"_type\":\"rootGraphFetchTree\",\"class\":\"meta::external::dataquality::tests::domain::Person\"}}]},{\"_type\":\"classInstance\",\"type\":\"rootGraphFetchTree\",\"value\":{\"subTrees\":[{\"_type\":\"propertyGraphFetchTree\",\"property\":\"name\"},{\"_type\":\"propertyGraphFetchTree\",\"property\":\"age\"}],\"_type\":\"rootGraphFetchTree\",\"class\":\"meta::external::dataquality::tests::domain::Person\"}}]},{\"fullPath\":\"meta::external::dataquality::tests::domain::dataqualitymappings\",\"_type\":\"packageableElementPtr\"},{\"_type\":\"classInstance\",\"type\":\"runtimeInstance\",\"value\":{\"runtime\":{\"_type\":\"legacyRuntime\",\"connections\":[{\"_type\":\"RelationalDatabaseConnection\",\"authenticationStrategy\":{\"_type\":\"h2Default\"},\"type\":\"H2\",\"datasourceSpecification\":{\"_type\":\"h2Local\"},\"element\":\"meta::external::dataquality::tests::domain::db\"}]}}}]}]}";

        assertLambda(validation, expected);
    }

    private void assertLambda(String modelString, String expected)
    {
        String lambdaJson = generateLambda(modelString);
        JsonAssert.assertJsonEquals(expected, lambdaJson, JsonAssert.when(Option.IGNORING_ARRAY_ORDER));
    }

    private void assertLambdaException(String modelString, String exception)
    {
        assertEquals(exception, assertThrows(PureAssertFailException.class, () -> generateLambda(modelString)).getInfo());
    }

    private String generateLambda(String modelString)
    {
        PureModelContextData modelData = loadWithModel(modelString);
        PureModel model = Compiler.compile(modelData, DeploymentMode.TEST_IGNORE_FUNCTION_MATCH, Identity.getAnonymousIdentity().getName());
        Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> routerExtensions = extensions();
        LambdaFunction<?> lambdaFunction = DataQualityLambdaGenerator.generateLambda(model, "meta::dataquality::Validation", null);
        return DataQualityLambdaGenerator.transformLambdaAsJson(lambdaFunction, model, routerExtensions);
    }

    private static PureModelContextData loadWithModel(String code)
    {
        return GrammarParseTestUtils.loadPureModelContextFromResources(
                FastList.newListWith(
                        "core_dataquality_test/dataquality_test_model.pure",
                        "core_dataquality_test/dataquality_test_model_legend.txt"),
                code, TestDataQualityLambdaGenerator.class);
    }

    private static Function<PureModel, RichIterable<? extends Root_meta_pure_extension_Extension>> extensions()
    {
        return (p) -> PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(p.getExecutionSupport()));
    }
}