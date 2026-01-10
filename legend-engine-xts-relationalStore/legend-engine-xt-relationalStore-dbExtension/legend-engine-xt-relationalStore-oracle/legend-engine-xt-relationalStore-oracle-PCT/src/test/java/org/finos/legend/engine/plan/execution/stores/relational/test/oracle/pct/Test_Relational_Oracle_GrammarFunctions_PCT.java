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

package org.finos.legend.engine.plan.execution.stores.relational.test.oracle.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalOraclePCTCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Oracle_GrammarFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final Adapter adapter = CoreRelationalOraclePCTCodeRepositoryProvider.oracleAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            // And
            pack("meta::pure::functions::boolean::tests::conjunctions::and", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),

            // Not
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotFalseExpression_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotFalse_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotInCollection_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: [false, false -> not()] -> at(1)\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotTrueExpression_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotTrue_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testOperatorScope_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),

            // Or
            pack("meta::pure::functions::boolean::tests::conjunctions::or", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),

            // Eq
            one("meta::pure::functions::boolean::tests::equality::eq::testEqBoolean_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqDate_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqEnum_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqFloat_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqInteger_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqPrimitiveExtension_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqString_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),

            // Equal
            pack("meta::pure::functions::boolean::tests::equality::equal", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDateStrictYear_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "\"Filter expressions are only supported for Primitives and Enums. Filter contains a parameter of type SideClass\"", AdapterQualifier.needsInvestigation),


            // GreaterThan / LessThan
            pack("meta::pure::functions::boolean::tests::inequalities::greaterThan", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::boolean::tests::inequalities::lessThan", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),
            pack("meta::pure::functions::boolean::tests::inequalities::lessThanEqual", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),

            // Filter
            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteralFromVar_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteral_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::filter::testLambdaAsFunctionParameter_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),

            // First
            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "\"Expected at most one object, but found many\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::first::testFirstOnOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::first::testFirstSimple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //Is Empty boolean
            pack("meta::pure::functions::collection::tests::isEmpty", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),

            // Map
            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "\"type not supported: meta::pure::functions::collection::tests::map::model::M_GeographicEntityType\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:63cc79-83); error compiling generated Java code:", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:52cc64-68); error compiling generated Java code:", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "\"Error during dynamic reactivation: Error dynamically evaluating value specification", AdapterQualifier.needsInvestigation),

            // Range
            pack("meta::pure::functions::collection::tests::range", "\"[unsupported-api] The function 'range' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // Size
            one("meta::pure::functions::collection::tests::size::testSizeEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::size::testSize_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // Compare
            pack("meta::pure::functions::lang::tests::compare", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testCompareDecimalAndLongTypes_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::compare::testDateCompare_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'compare_T_1__T_1__Integer_1_'", AdapterQualifier.unsupportedFeature),

            // Minus
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "\"\nexpected: -4.0D\nactual:   -4D\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testLargeMinus_Function_1__Boolean_1_", "For input string: \"-9223372036854775850\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'genericType_Any_MANY__GenericType_1_'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),

            // Plus
            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\"\nexpected: 6.0D\nactual:   6D\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "For input string: \"9223372036854775826\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'genericType_Any_MANY__GenericType_1_'.", AdapterQualifier.needsInvestigation),

            // Times
            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\"\nexpected: 353791.470D\nactual:   353791.47\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "For input string: \"18446744073709551614\"", AdapterQualifier.needsInvestigation),

            // Plus (String)
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithPropertyExpressions_Function_1__Boolean_1_", "\"type not supported: meta::pure::functions::string::tests::plus::model::P_GeographicEntityType\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'at_T_MANY__Integer_1__T_1_'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "\"Match failure: StoreMappingClusteredValueSpecificationObject instanceOf StoreMappingClusteredValueSpecification\"", AdapterQualifier.needsInvestigation),

            // Let
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "\"type not supported: meta::pure::functions::lang::tests::model::LA_GeographicEntityType\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::letFn::testLetInsideIf_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'letFunction_String_1__T_m__T_m_'", AdapterQualifier.unsupportedFeature)


    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Oracle).getFirst())
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
