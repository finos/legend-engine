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
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_DuckDB_GrammarFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.duckDBAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Not
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotInCollection_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: [false, false -> not()] -> at(1)\""),

            // Eq
            one("meta::pure::functions::boolean::tests::equality::eq::testEqDate_Function_1__Boolean_1_", "\"DuckDB doesn't support YEAR and YEAR-MONTH\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "Error while executing: insert into leSchema.SideClass (_pureId,stringId,intId) values (10,'firstSide',1);"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 1 where the collection is of size 1\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqEnum_Function_1__Boolean_1_", "\"Assert failed\""),

            // Equal
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualEnum_Function_1__Boolean_1_", "\"Assert failed\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "Error while executing: insert into leSchema.SideClass (_pureId,stringId,intId) values (10,'firstSide',1);"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 1 where the collection is of size 1\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDateStrictYear_Function_1__Boolean_1_", "\"DuckDB doesn't support YEAR and YEAR-MONTH\""),

            // GreaterThan
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "\"Assert failed\""),

            // GreaterThanEqual
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "\"Assert failed\""),

            // Filter
            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/filter.pure:49cc46-50); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteralFromVar_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteral_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::filter::testLambdaAsFunctionParameter_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),

            // First
            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "\"Expected at most one object, but found many\""),
            one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::first::testFirstSimple_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\""),

            // Map
            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:63cc79-83); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:52cc64-68); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "\"Error during dynamic reactivation: Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:43cc92-98); error compiling generated Java code:"),

            // Range
            one("meta::pure::functions::collection::tests::range::testRangeWithStep_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::range::testRangeWithVariables_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::range::testRange_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::range::testReverseRange_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Size
            one("meta::pure::functions::collection::tests::size::testSizeEmpty_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'size_Any_MANY__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::size::testSize_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'size_Any_MANY__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Compare
            pack("meta::pure::functions::lang::tests::compare", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testCompareDecimalAndLongTypes_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testDateCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Minus
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "\"\nexpected: -4.0D\nactual:   -4.0\""),
            one("meta::pure::functions::math::tests::minus::testLargeMinus_Function_1__Boolean_1_", "java.sql.SQLException: Out of Range Error: Overflow in subtraction of INT64 (-9223372036854775718 - 132)!"),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "\"Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy\""),
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "java.sql.SQLException: java.sql.SQLException: Parser Error: SELECT clause without selection list"),

            // Plus
            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\"\nexpected: 6.0D\nactual:   6.0\""),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "java.sql.SQLException: Out of Range Error: Overflow in addition of INT64 (9223372036854775807 + 3)!"),
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "\"Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy\""),

            // Times
            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\"\nexpected: 353791.470D\nactual:   353791.47\""),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "java.sql.SQLException: Out of Range Error: Overflow in multiplication of INT64 (2 * 9223372036854775807)!"),

            // Plus (String)
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithPropertyExpressions_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'at_T_MANY__Integer_1__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "\"Match failure: StoreMappingClusteredValueSpecificationObject instanceOf StoreMappingClusteredValueSpecification\""),

            // Let
            one("meta::pure::functions::lang::tests::letFn::testAssignLiteralToVariable_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\""),
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::lang::tests::letFn::testLetInsideIf_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"")

    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.DuckDB).getFirst())
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
