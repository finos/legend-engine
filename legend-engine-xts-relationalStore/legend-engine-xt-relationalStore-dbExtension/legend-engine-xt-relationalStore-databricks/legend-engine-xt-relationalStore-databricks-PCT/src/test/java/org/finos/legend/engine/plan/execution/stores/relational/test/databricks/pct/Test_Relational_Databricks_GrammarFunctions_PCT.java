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
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
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
            one("meta::pure::functions::boolean::tests::conjunctions::and::testBinaryTruthTable_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true AND true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 8", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testShortCircuitSimple_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_DIFF_TYPES] Cannot resolve \"(false AND (((1.0 * 12) / 0) > 0))\" due to data type mismatch: the left and right operands of the binary operator have incompatible types (\"STRING\" and \"BOOLEAN\").", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testTernaryTruthTable_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true AND true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 9", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testTernaryExpressions_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_DIFF_TYPES] Cannot resolve \"(((1 = 1) AND (NOT (2 = 3))) AND true)\" due to data type mismatch: the left and right operands of the binary operator have incompatible types (\"BOOLEAN\" and \"STRING\")", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotInCollection_Function_1__Boolean_1_", "->at(...) function is supported only after direct access of 1->MANY properties. Current expression: [false, false -> not()] -> at(1)", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testOperatorScope_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE] Cannot resolve \"(NOT false)\" due to data type mismatch: The first parameter requires the \"BOOLEAN\" type, however \"false\" has the type \"STRING\".", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotFalse_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE] Cannot resolve \"(NOT false)\" due to data type mismatch: The first parameter requires the \"BOOLEAN\" type, however \"false\" has the type \"STRING\".", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotTrue_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE] Cannot resolve \"(NOT true)\" due to data type mismatch: The first parameter requires the \"BOOLEAN\" type, however \"true\" has the type \"STRING\".", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testBinaryTruthTable_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true OR true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 8", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testTernaryTruthTable_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_WRONG_TYPE] Cannot resolve \"(true OR true)\" due to data type mismatch: the binary operator requires the input type \"BOOLEAN\", not \"STRING\". SQLSTATE: 42K09; line 1 pos 9", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testShortCircuitSimple_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_DIFF_TYPES] Cannot resolve \"(true OR (((1.0 * 12) / 0) > 0))\" due to data type mismatch: the left and right operands of the binary operator have incompatible types (\"STRING\" and \"BOOLEAN\").", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testTernaryExpressions_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.BINARY_OP_DIFF_TYPES] Cannot resolve \"(((NOT (1 = 1)) OR (2 = 3)) OR true)\" due to data type mismatch: the left and right operands of the binary operator have incompatible types (\"BOOLEAN\" and \"STRING\").", AdapterQualifier.needsInvestigation),

            //equality
            one("meta::pure::functions::boolean::tests::equality::eq::testEqDate_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqEnum_Function_1__Boolean_1_", "Assert failed", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqPrimitiveExtension_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_",  "Error in 'meta::relational::tests::pct::process::myMapping': Can't find the main table for class 'BottomClass'. Please specify a main table using the ~mainTable directive.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDateStrictYear_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualEnum_Function_1__Boolean_1_", "Assert failed", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualPrimitiveExtension_Function_1__Boolean_1_", "Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "Error in 'meta::relational::tests::pct::process::myMapping': Can't find the main table for class 'BottomClass'. Please specify a main table using the ~mainTable directive.", AdapterQualifier.needsInvestigation),

            //inequalities
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE] Cannot resolve \"(NOT true)\" due to data type mismatch: The first parameter requires the \"BOOLEAN\" type, however \"true\" has the type \"STRING\".", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE] Cannot resolve \"(NOT true)\" due to data type mismatch: The first parameter requires the \"BOOLEAN\" type, however \"true\" has the type \"STRING\".", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_Boolean_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE] Cannot resolve \"(NOT false)\" due to data type mismatch: The first parameter requires the \"BOOLEAN\" type, however \"false\" has the type \"STRING\".", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_Boolean_Function_1__Boolean_1_", "[DATATYPE_MISMATCH.UNEXPECTED_INPUT_TYPE] Cannot resolve \"(NOT false)\" due to data type mismatch: The first parameter requires the \"BOOLEAN\" type, however \"false\" has the type \"STRING\".", AdapterQualifier.needsInvestigation),

            //filter
            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/filter.pure:51cc46-50); error compiling generated Java code", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteralFromVar_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Databricks", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteral_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Databricks", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::filter::testLambdaAsFunctionParameter_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: Databricks", AdapterQualifier.needsInvestigation),

            //first
            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "Expected at most one object, but found many", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::first::testFirstSimple_Function_1__Boolean_1_", "Cannot cast a collection of size 2 to multiplicity [1]", AdapterQualifier.needsInvestigation),

            //map
            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:65cc79-83); error compiling generated Java code", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:54cc64-68); error compiling generated Java code", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "Error during dynamic reactivation: Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:45cc92-98); error compiling generated Java code", AdapterQualifier.unsupportedFeature),

            //range
            one("meta::pure::functions::collection::tests::range::testRangeWithStep_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::range::testRangeWithVariables_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::range::testRange_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::range::testReverseRange_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::range::testRangeWithStartStopEqual_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::range::testReverseRangeWithPositiveStep_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::range::testReverseRangeWithStep_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            //size
            one("meta::pure::functions::collection::tests::size::testSize_Function_1__Boolean_1_", "[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),

            //compare
            one("meta::pure::functions::lang::tests::compare::testBooleanCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testCompareDecimalAndLongTypes_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testCompareMixedTypes_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testDateCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testFloatCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testIntegerCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testNumberCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testStringCompare_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),

            //letFn
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::letFn::testLetInsideIf_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),

            //divide
            one("meta::pure::functions::math::tests::divide::testDivideWithNonTerminatingExpansion_Function_1__Boolean_1_", "\nexpected: 0.010416666666666666\nactual:   0.010417", AdapterQualifier.needsInvestigation),

            //minus
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "\nexpected: -4.0D\nactual:   -4D", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] org.apache.spark.sql.catalyst.parser.ParseException: \n[PARSE_SYNTAX_ERROR] Syntax error at or near end of input. SQLSTATE: 42601 (line 1, pos 10)", AdapterQualifier.needsInvestigation),

            //plus
            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\nexpected: 6.0D\nactual:   6D", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "\"\nexpected: -1\nactual:   -9223372036854775790\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy", AdapterQualifier.needsInvestigation),

            //times
            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\nexpected: 353791.470D\nactual:   353791.47", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "\"\nexpected: -1\nactual:   -2\"", AdapterQualifier.needsInvestigation),

            // map / variant
            one("meta::pure::functions::collection::tests::map::testMap_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::map::testMap_FromVariant_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),

            // filter / variant
            one("meta::pure::functions::collection::tests::filter::testFilter_FromVariant_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::filter::testFilter_FromVariantAsPrimitive_Function_1__Boolean_1_", "[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),

            //string-plus
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithPropertyExpressions_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'at_T_MANY__Integer_1__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "Match failure: StoreMappingClusteredValueSpecificationObject instanceOf StoreMappingClusteredValueSpecification", AdapterQualifier.needsInvestigation)
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
