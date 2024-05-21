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

package org.finos.legend.engine.plan.execution.stores.relational.test.duckdb.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalduckdbCodeRepositoryProvider;
import org.finos.legend.pure.code.core.FunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_DuckDB_BaseFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = FunctionsCodeRepositoryProvider.baseFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.duckDBAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::collection::tests::contains::testContainsPrimitive_Function_1__Boolean_1_", "\"Match failure: 2014-02-01 instanceOf StrictDate\""),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 1 where the collection is of size 1\""),

            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'new_Class_1__String_1__KeyExpression_MANY__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),

            one("meta::pure::functions::collection::tests::exists::testExistsInSelect_Function_1__Boolean_1_", "\"Error mapping not found for class CO_Firm cache:''\""),
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Error mapping not found for class CO_Firm cache:''\""),

            one("meta::pure::functions::collection::tests::find::testFindInstance_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::collection::tests::find::testFindLiteralFromVar_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'find_T_MANY__Function_1__T_$0_1$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::find::testFindLiteral_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'find_T_MANY__Function_1__T_$0_1$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::find::testFindUsingVarForFunction_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),

            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "\"Error mapping not found for class Nil cache:''\""),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsFalse_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsTrue_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            one("meta::pure::functions::collection::tests::indexof::testIndexOfOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'indexOf' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::indexof::testIndexOf_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'indexOf_T_MANY__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::last::testLastOfOneElementList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::last::testLast_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            one("meta::pure::functions::collection::tests::reverse::testReverseEmpty_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::reverse::testReverse_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'reverse_T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            one("meta::pure::functions::collection::tests::slice::testSliceEqualBounds_Function_1__Boolean_1_", "\"Cannot cast a collection of size 4 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'new_Class_1__String_1__KeyExpression_MANY__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::slice::testSliceOnBounds_Function_1__Boolean_1_", "\"Cannot cast a collection of size 4 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::slice::testSliceOnEmpty_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::slice::testSliceOutOfBounds_Function_1__Boolean_1_", "\"Cannot cast a collection of size 4 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::slice::testSlice_Function_1__Boolean_1_", "\"Cannot cast a collection of size 6 to multiplicity [1]\""),

            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'new_Class_1__String_1__KeyExpression_MANY__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnEmptyList_Function_1__Boolean_1_", "\"getAll_Class_1__T_MANY_ is prohibited!\""),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnNonEmptyList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),

            one("meta::pure::functions::collection::tests::zip::testZipBothListsAreOfPairs_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsSameLength_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListsIsOfPairs_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListsIsOfPairs_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"")

    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource)TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.DuckDB).getFirst())
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
