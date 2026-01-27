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

package org.finos.legend.engine.plan.execution.stores.relational.test.sqlserver.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalSqlServerCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_SqlServer_GrammarFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final Adapter adapter = CoreRelationalSqlServerCodeRepositoryProvider.sqlserverAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // And
            one("meta::pure::functions::boolean::tests::conjunctions::and::testBinaryExpressions_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testBinaryTruthTable_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'and'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testShortCircuitSimple_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'and'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testTernaryExpressions_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testTernaryTruthTable_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'and'.", AdapterQualifier.needsInvestigation),

            // Not
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotFalseExpression_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotFalse_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotTrueExpression_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotTrue_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testOperatorScope_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotInCollection_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: [false, false -> not()] -> at(1)\""),

            // Or
            one("meta::pure::functions::boolean::tests::conjunctions::or::testBinaryExpressions_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testBinaryTruthTable_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'or'."),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testShortCircuitSimple_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'or'."),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testTernaryExpressions_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testTernaryTruthTable_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'or'."),

            // Eq
            one("meta::pure::functions::boolean::tests::equality::eq::testEqBoolean_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqEnum_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqFloat_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqInteger_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqPrimitiveExtension_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqString_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqDate_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),

            // Equal
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualBoolean_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDate_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualEnum_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualFloat_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualInteger_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualString_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDateStrictYear_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualPrimitiveExtension_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),

            // GreaterThan
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Date_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '>'."),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Number_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '>'."),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_String_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '>'."),

            // GreaterThanEqual
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Date_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '>'."),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Number_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '>'."),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_String_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '>'."),

            // LessThan
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_Boolean_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_Date_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_Number_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_String_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),

            // LessThanEqual
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_Boolean_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_Date_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_Number_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_String_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '<'."),

            // Filter
            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/filter.pure:49cc46-50); error compiling generated Java code", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteralFromVar_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteral_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::filter::testLambdaAsFunctionParameter_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsInvestigation),

            // First
            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "Expected at most one object, but found many", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::first::testFirstSimple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::first::testFirstOnOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // IsEmpty
            one("meta::pure::functions::collection::tests::isEmpty::testIsEmptyFalseMultiple_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::collection::tests::isEmpty::testIsEmptyFalseSingle_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),

            // Map
            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "type not supported: meta::pure::functions::collection::tests::map::model::M_GeographicEntityType", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:63cc79-83); error compiling generated Java code:", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:52cc64-68); error compiling generated Java code:", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "Error during dynamic reactivation: Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:43cc92-98); error compiling generated Java code:", AdapterQualifier.unsupportedFeature),

            // Range
            one("meta::pure::functions::collection::tests::range::testRangeWithStep_Function_1__Boolean_1_", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::range::testRangeWithVariables_Function_1__Boolean_1_", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::range::testRange_Function_1__Boolean_1_", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::range::testReverseRange_Function_1__Boolean_1_", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::range::testRangeWithStartStopEqual_Function_1__Boolean_1_", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::range::testReverseRangeWithPositiveStep_Function_1__Boolean_1_", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::range::testReverseRangeWithStep_Function_1__Boolean_1_", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            //size
            one("meta::pure::functions::collection::tests::size::testSize_Function_1__Boolean_1_", "[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::size::testSizeEmpty_Function_1__Boolean_1_", "[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),

            // Compare
            pack("meta::pure::functions::lang::tests::compare", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testCompareDecimalAndLongTypes_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testDateCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // Divide
            one("meta::pure::functions::math::tests::divide::testDivideWithNonTerminatingExpansion_Function_1__Boolean_1_", "\"\nexpected: 0.010416666666666666\nactual:   0.010416\""),
            one("meta::pure::functions::math::tests::divide::testDecimalDivide_Function_1__Boolean_1_", "\"\nexpected: 0.5238095238095238\nactual:   0.523809\""),

            // Minus
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "\"\nexpected: -4.0D\nactual:   -4.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testLargeMinus_Function_1__Boolean_1_", "For input string: \"-9223372036854775850\""),
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near 'select'."),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'genericType_Any_MANY__GenericType_1_'", AdapterQualifier.needsInvestigation),

            // Plus
            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\"\nexpected: 6.0D\nactual:   6.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "For input string: \"9223372036854775826\""),
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'genericType_Any_MANY__GenericType_1_'", AdapterQualifier.needsInvestigation),

            // Times
            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\"\nexpected: 353791.470D\nactual:   353791.47\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "For input string: \"18446744073709551614\""),

            // Plus (String)
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithPropertyExpressions_Function_1__Boolean_1_", "type not supported: meta::pure::functions::string::tests::plus::model::P_GeographicEntityType", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'at_T_MANY__Integer_1__T_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "Match failure: StoreMappingClusteredValueSpecificationObject instanceOf StoreMappingClusteredValueSpecification", AdapterQualifier.needsInvestigation),

            // Let
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "type not supported: meta::pure::functions::lang::tests::model::LA_GeographicEntityType", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::letFn::testLetInsideIf_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // isEmpty / isNotEmpty / variant
            one("meta::pure::functions::collection::tests::isEmpty::testIsEmpty_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::collection::tests::isEmpty::testIsNotEmptyFalse_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::collection::tests::isEmpty::testIsNotEmptyMultiple_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'not'."),
            one("meta::pure::functions::collection::tests::isEmpty::testIsNotEmptySingle_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'.")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.SqlServer).getFirst())
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
