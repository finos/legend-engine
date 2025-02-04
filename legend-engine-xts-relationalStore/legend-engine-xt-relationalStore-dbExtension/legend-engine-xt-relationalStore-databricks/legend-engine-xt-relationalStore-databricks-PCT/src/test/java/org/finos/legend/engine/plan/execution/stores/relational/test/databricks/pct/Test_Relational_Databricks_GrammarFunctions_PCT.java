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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks.pct;

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

public class Test_Relational_Databricks_GrammarFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.databricksAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //conjunctions
            one("meta::pure::functions::boolean::tests::conjunctions::and::testBinaryTruthTable_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true AND true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 8"),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testShortCircuitSimple_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [DIVIDE_BY_ZERO] org.apache.spark.SparkArithmeticException: [DIVIDE_BY_ZERO] Division by zero. Use `try_divide` to tolerate divisor being 0 and return NULL instead. If necessary set \"ansi_mode\" to \"false\" to bypass this error. SQLSTATE: 22012"),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testTernaryTruthTable_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true AND true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 9"),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotInCollection_Function_1__Boolean_1_", "->at(...) function is supported only after direct access of 1->MANY properties. Current expression: [false, false -> not()] -> at(1)"),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testOperatorScope_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true AND false)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 12"),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testBinaryTruthTable_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true OR true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 8"),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testTernaryTruthTable_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true OR true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 9"),

            //equality
            one("meta::pure::functions::boolean::tests::equality::eq::testEqDate_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic."),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqEnum_Function_1__Boolean_1_", "Assert failed"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::boolean::eq(SideClass[*],SideClass[*])'"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqPrimitiveExtension_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::boolean::eq(SideClass[*],Nil[1])'"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "The system is trying to get an element at offset 1 where the collection is of size 1"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDateStrictYear_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic."),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualEnum_Function_1__Boolean_1_", "Assert failed"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualPrimitiveExtension_Function_1__Boolean_1_", "Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "The system is trying to get an element at offset 1 where the collection is of size 1"),

            //inequalities
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "Assert failed"),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "Assert failed"),

            //filter
            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/filter.pure:49cc46-50); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteralFromVar_Function_1__Boolean_1_", "Cannot cast a collection of size 3 to multiplicity [1]"),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteral_Function_1__Boolean_1_", "Cannot cast a collection of size 3 to multiplicity [1]"),
            one("meta::pure::functions::collection::tests::filter::testLambdaAsFunctionParameter_Function_1__Boolean_1_", "Cannot cast a collection of size 3 to multiplicity [1]"),

            //first
            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "Expected at most one object, but found many"),
            one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery"),
            one("meta::pure::functions::collection::tests::first::testFirstSimple_Function_1__Boolean_1_", "Cannot cast a collection of size 2 to multiplicity [1]"),

            //map
            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:63cc79-83); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:52cc64-68); error compiling generated Java code:"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "Error during dynamic reactivation: Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:43cc92-98); error compiling generated Java code:"),

            //range
            one("meta::pure::functions::collection::tests::range::testRangeWithStep_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::collection::tests::range::testRangeWithVariables_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::collection::tests::range::testRange_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::collection::tests::range::testReverseRange_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),

            //size
            one("meta::pure::functions::collection::tests::size::testSizeEmpty_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'size_Any_MANY__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::collection::tests::size::testSize_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'size_Any_MANY__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),

            //compare
            one("meta::pure::functions::lang::tests::compare::testBooleanCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::compare::testCompareDecimalAndLongTypes_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::compare::testCompareMixedTypes_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::compare::testDateCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::compare::testFloatCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::compare::testIntegerCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::compare::testNumberCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::compare::testStringCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),

            //letFn
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]"),
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]"),
            one("meta::pure::functions::lang::tests::letFn::testLetInsideIf_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),

            //divide
            one("meta::pure::functions::math::tests::divide::testDivideWithNonTerminatingExpansion_Function_1__Boolean_1_", "\nexpected: 0.010416666666666666\nactual:   0.010417"),

            //minus
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "\nexpected: -4.0D\nactual:   -4.0"),
            one("meta::pure::functions::math::tests::minus::testLargeMinus_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [ARITHMETIC_OVERFLOW] org.apache.spark.SparkArithmeticException: [ARITHMETIC_OVERFLOW] long overflow. Use 'try_subtract' to tolerate overflow and return NULL instead. If necessary set \"ansi_mode\" to \"false\" to bypass this error. SQLSTATE: 22003"),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy"),
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [PARSE_SYNTAX_ERROR] org.apache.spark.sql.catalyst.parser.ParseException: \n[PARSE_SYNTAX_ERROR] Syntax error at or near end of input. SQLSTATE: 42601 (line 1, pos 10)"),

            //plus
            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\nexpected: 6.0D\nactual:   6.0"),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [ARITHMETIC_OVERFLOW] org.apache.spark.SparkArithmeticException: [ARITHMETIC_OVERFLOW] long overflow. Use 'try_add' to tolerate overflow and return NULL instead. If necessary set \"ansi_mode\" to \"false\" to bypass this error. SQLSTATE: 22003"),
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy"),

            //times
            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\nexpected: 353791.470D\nactual:   353791.47"),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [ARITHMETIC_OVERFLOW] org.apache.spark.SparkArithmeticException: [ARITHMETIC_OVERFLOW] long overflow. Use 'try_multiply' to tolerate overflow and return NULL instead. If necessary set \"ansi_mode\" to \"false\" to bypass this error. SQLSTATE: 22003"),

            //string-plus
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithPropertyExpressions_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]"),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'at_T_MANY__Integer_1__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki."),
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "Match failure: StoreMappingClusteredValueSpecificationObject instanceOf StoreMappingClusteredValueSpecification")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Databricks).getFirst())
        );
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
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
