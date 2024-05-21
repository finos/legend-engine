// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_H2_BasicFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.basicFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.H2Adapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Add
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'add_T_MANY__Integer_1__T_1__T_$1_MANY$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::add::testAdd_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'add_T_MANY__T_1__T_$1_MANY$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),

            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldCollectionAccumulator_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "\"Error mapping not found for class Any cache:''\""),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndNonEmptyIdentity_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::collection::tests::fold::testFoldMixedAccumulatorTypes_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::collection::tests::fold::testFoldWithEmptyAccumulator_Function_1__Boolean_1_", "\"Error mapping not found for class Nil cache:''\""),
            one("meta::pure::functions::collection::tests::fold::testFoldWithSingleValue_Function_1__Boolean_1_", "\"Error mapping not found for class Nil cache:''\""),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::collection::tests::fold::testIntegerSum_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::fold::testStringSum_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Init
            one("meta::pure::functions::collection::tests::init::testInitOnEmptySet_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::init::testInitOneElement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'init_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::init::testInit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'init_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Sort
            one("meta::pure::functions::collection::tests::sort::testMixedSortNoComparator_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortNoComparator_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortReversed_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithFunctionVariables_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithKey_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSort_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSortEmptySet_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Tail
            one("meta::pure::functions::collection::tests::tail::testTailOnEmptySet_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::tail::testTailOneElement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::tail::testTail_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.H2).getFirst())
        );
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
