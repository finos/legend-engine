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
import org.finos.legend.pure.code.core.FunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_BaseFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = FunctionsCodeRepositoryProvider.baseFunctions;
    private static final String adapter = "meta::pure::executionPlan::platformBinding::legendJava::pct::testAdapterForJavaBindingExecution_Function_1__X_o_";
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::contains::ClassWithoutEquality\""),
            // Drop
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/executionPlan/platformBinding/typeInfo/typeInfo.pure line:493 column:61), \"Cannot cast a collection of size 0 to multiplicity [1]\""),
            // Exists
            one("meta::pure::functions::collection::tests::exists::testExistsInSelect_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),
            // Find (Not Supported Yet)
            pack("meta::pure::functions::collection::tests::find", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::collection::find_T_MANY__Function_1__T_$0_1$_ is not supported yet!\""),
            // ForAll
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/pct_java.pure lines:19c87-40c1), \"Unexpected error executing function with params [Anonymous_Lambda]\""),
            // Last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/pct_java.pure lines:19c87-40c1), \"Unexpected error executing function with params [Anonymous_Lambda]\""),
            // Slice
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/executionPlan/platformBinding/typeInfo/typeInfo.pure line:493 column:61), \"Cannot cast a collection of size 0 to multiplicity [1]\""),
            // Take
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/executionPlan/platformBinding/typeInfo/typeInfo.pure line:493 column:61), \"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/pct_java.pure lines:19c87-40c1), \"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/pct_java.pure lines:19c87-40c1), \"Unexpected error executing function with params [Anonymous_Lambda]\""),
            // Zip
            pack("meta::pure::functions::collection::tests::zip", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_ is not supported yet!\"")
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
