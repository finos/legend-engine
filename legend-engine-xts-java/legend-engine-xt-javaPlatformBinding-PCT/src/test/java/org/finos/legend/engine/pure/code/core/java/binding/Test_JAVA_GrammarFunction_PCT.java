// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.java.binding;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.config.PCTReport;
import org.finos.legend.pure.m3.pct.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_GrammarFunction_PCT extends PCTReport
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final String adapter = "meta::pure::executionPlan::platformBinding::legendJava::pct::testAdapterForJavaBindingExecution_Function_1__X_o_";
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
                // Eq
                one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::SideClass\""),
                one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::BottomClass\""),

                // Equal
                one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::SideClass\""),
                one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::BottomClass\""),

                // GreaterThan
                one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Assert failed\""),

                // GreaterThanEqual
                one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Assert failed\""),

                // Filter
                one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Person\""),

                // First
                one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),
                one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/pct_java.pure lines:19c87-40c1), \"Unexpected error executing function with params [Anonymous_Lambda]\""),

                // Map
                one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::map::model::M_Person\""),
                one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::map::model::M_Person\""),
                one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::map::model::M_Person\""),
                one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/router/platform/routing.pure line:78 column:8), \"Match failure: NoSetRoutedValueSpecificationObject instanceOf NoSetRoutedValueSpecification\""),

                // Divide
                one("meta::pure::functions::math::tests::divide::testDecimalDivide_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:21 column:5), \"\nexpected: -31.42D\nactual:   -31.42\""),

                // Minus
                one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "class java.lang.Double cannot be cast to class java.math.BigDecimal (java.lang.Double and java.math.BigDecimal are in module java.base of loader 'bootstrap')"),
                one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/router/platform/routing.pure line:78 column:8), \"Match failure: NoSetRoutedValueSpecificationObject instanceOf NoSetRoutedValueSpecification\""),
                one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/pct_java.pure lines:19c87-40c1), \"Unexpected error executing function with params [Anonymous_Lambda]\""),

                // Plus
                one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "class java.lang.Double cannot be cast to class java.math.BigDecimal (java.lang.Double and java.math.BigDecimal are in module java.base of loader 'bootstrap')"),
                one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:21 column:5), \"\nexpected: -1\nactual:   -9223372036854775790\""),
                one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/router/platform/routing.pure line:78 column:8), \"Match failure: NoSetRoutedValueSpecificationObject instanceOf NoSetRoutedValueSpecification\""),

                // Times
                one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:21 column:5), \"\nexpected: 353791.470D\nactual:   353791.47\""),
                one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:21 column:5), \"\nexpected: -1\nactual:   -2\""),
                one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:21 column:5), \"\nexpected: -1\nactual:   -2\""),

                // String plus
                one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::string::tests::plus::model::P_Person\""),
                one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::string::tests::plus::model::P_Person\"")

    );

    public static Test suite()
    {
        return PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter);
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public String getAdapter()
    {
        return adapter;
    }

    @Override
    public String getPlatform()
    {
        return platform;
    }
}
