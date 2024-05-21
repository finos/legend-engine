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
import org.finos.legend.pure.code.core.CoreJavaPlatformBindingCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_BasicFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.basicFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Add
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::collection::add_T_MANY__Integer_1__T_1__T_$1_MANY$_ is not supported yet!\""),

            // Concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/platform/executionPlanNodes/platformUnion/platformUnion.pure line:24 column:112), \"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/platform/executionPlanNodes/platformUnion/platformUnion.pure line:24 column:112), \"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "Execution error at (resource:/core_java_platform_binding/legendJavaPlatformBinding/platform/executionPlanNodes/platformUnion/platformUnion.pure line:24 column:112), \"The system is trying to get an element at offset 0 where the collection is of size 0\""),

            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Assert failed\""),
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::fold::FO_Person\""),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::fold::FO_Person\""),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/basics/tests/assert.pure line:26 column:5), \"Unhandled value type: meta::pure::functions::collection::tests::fold::FO_Person\""),

            // Sort
            one("meta::pure::functions::collection::tests::sort::testMixedSortNoComparator_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/platform/executionPlan/executionPlan_generation.pure line:55 column:62), \"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortNoComparator_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/platform/executionPlan/executionPlan_generation.pure line:55 column:62), \"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortReversed_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/platform/executionPlan/executionPlan_generation.pure line:55 column:62), \"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSort_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/platform/executionPlan/executionPlan_generation.pure line:55 column:62), \"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSortEmptySet_Function_1__Boolean_1_", "Execution error at (resource:/core/pure/platform/executionPlan/executionPlan_generation.pure line:55 column:62), \"Cannot cast a collection of size 0 to multiplicity [1]\"")
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
    public Adapter getAdapter()
    {
        return adapter;
    }

    @Override
    public String getPlatform()
    {
        return platform;
    }
}
