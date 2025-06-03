// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.spanner.pct;

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

public class Test_Relational_Spanner_GrammarFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.spannerAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //and
            one("meta::pure::functions::boolean::tests::conjunctions::and::testShortCircuitSimple_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: [ERROR] division by zero - Statement: 'select (Boolean'false' and ((1.0 * 12) / 0) > 0)'"),

            //not
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotInCollection_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: [false, false -> not()] -> at(1)\""),

            //or
            one("meta::pure::functions::boolean::tests::conjunctions::or::testShortCircuitSimple_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: [ERROR] division by zero - Statement: 'select (Boolean'true' or ((1.0 * 12) / 0) > 0)'"),

            //eq
            one("meta::pure::functions::boolean::tests::equality::eq::testEqDate_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqEnum_Function_1__Boolean_1_", "\"Assert failed\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqPrimitiveExtension_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "Error in 'meta::relational::tests::pct::process::myMapping': Can't find the main table for class 'BottomClass'. Please specify a main table using the ~mainTable directive."),

            //equal
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDateStrictYear_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDate_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: The Postgres Type is not supported: timestamp without time zone - Statement: 'select Timestamp'2014-02-27 05:01:35.231' = Timestamp'2014-02-27 05:01:35.231''"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualPrimitiveExtension_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "Error in 'meta::relational::tests::pct::process::myMapping': Can't find the main table for class 'BottomClass'. Please specify a main table using the ~mainTable directive."),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualEnum_Function_1__Boolean_1_", "\"Assert failed\""),

            //greaterThan
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "\"Assert failed\""),

            //greaterThanEqual
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "\"Assert failed\""),

            //filter
            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/filter.pure:51cc46-50); error compiling generated Java code"),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteralFromVar_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteral_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::filter::testLambdaAsFunctionParameter_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),

            //first
            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "\"Expected at most one object, but found many\""),
            one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::first::testFirstSimple_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\""),

            //isEmpty
            one("meta::pure::functions::collection::tests::isEmpty::testIsEmpty_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Unable to resolve argument type. Please consider adding an explicit cast - Statement: 'select null is null'"),

            //map
            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:65cc79-83); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:54cc64-68); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "Error during dynamic reactivation: Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:45cc92-98); error compiling generated Java code:"),

            //range
            one("meta::pure::functions::collection::tests::range::testRangeWithStep_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::range::testRangeWithVariables_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::range::testRange_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::range::testReverseRange_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //size
            one("meta::pure::functions::collection::tests::size::testSizeEmpty_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'size_Any_MANY__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::size::testSize_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'size_Any_MANY__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //compare
            one("meta::pure::functions::lang::tests::compare::testBooleanCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testCompareDecimalAndLongTypes_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testCompareMixedTypes_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testDateCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testFloatCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testIntegerCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testNumberCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::compare::testStringCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //letFn
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::lang::tests::letFn::testLetInsideIf_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //divide
            one("meta::pure::functions::math::tests::divide::testDecimalDivide_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] function round(numeric, bigint) does not exist\nHint: No function matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select -round(((1.0 * 3.1415) / 0.1),2)'"),

            //minus
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: Statement must produce at least one output column - Statement: 'select --1'"),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "\"Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy\""),
            one("meta::pure::functions::math::tests::minus::testLargeMinus_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: int64 overflow: -9223372036854775718 - 132 - Statement: 'select (-9223372036854775718 - 132)'"),
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "\"\nexpected: -4.0D\nactual:   -4D\""),

            //plus
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "\"Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy\""),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: int64 overflow: 9223372036854775807 + 3 - Statement: 'select (9223372036854775807 + 3 + (4 + 5) + 7)'"),
            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\"\nexpected: 6.0D\nactual:   6D\""),

            //times
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: OUT_OF_RANGE: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: OUT_OF_RANGE: int64 overflow: 2 * 9223372036854775807 - Statement: 'select (2 * 9223372036854775807)'"),
            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\"\nexpected: 353791.470D\nactual:   353791.47\""),

            // variant
            one("meta::pure::functions::collection::tests::filter::testFilter_FromVariantAsPrimitive_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::variant::convert::toVariant'"),
            one("meta::pure::functions::collection::tests::filter::testFilter_FromVariant_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::variant::convert::toVariant'"),
            one("meta::pure::functions::collection::tests::map::testMap_FromVariantAsPrimitive_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::variant::convert::toVariant'"),
            one("meta::pure::functions::collection::tests::map::testMap_FromVariant_Function_1__Boolean_1_", "Can't resolve the builder for function 'meta::pure::functions::variant::convert::toVariant'"),

            //string-plus
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithFunctionExpressions_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toString' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithPropertyExpressions_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'at_T_MANY__Integer_1__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "\"Match failure: StoreMappingClusteredValueSpecificationObject instanceOf StoreMappingClusteredValueSpecification\"")

            );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Spanner).getFirst())
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
