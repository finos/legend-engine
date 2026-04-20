// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.test.deephaven.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.TestDeephavenConnectionIntegrationLoader;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreDeephavenPCTCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Deephaven_GrammarFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final Adapter adapter = CoreDeephavenPCTCodeRepositoryProvider.deephavenAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::pure::functions::boolean::tests::conjunctions::and::testBinaryExpressions_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testBinaryTruthTable_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testShortCircuitSimple_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testTernaryExpressions_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::and::testTernaryTruthTable_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotFalseExpression_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::not_Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotFalse_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::not_Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotInCollection_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::at_T_MANY__Integer_1__T_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotTrueExpression_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::not_Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testNotTrue_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::not_Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::not::testOperatorScope_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::conjunctions::or::testBinaryExpressions_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testBinaryTruthTable_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testShortCircuitSimple_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testTernaryExpressions_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::conjunctions::or::testTernaryTruthTable_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::equality::eq::testEqBoolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqDate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqEnum_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqFloat_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqInteger_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqPrimitiveExtension_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqString_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::eq_Any_1__Any_1__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::equality::equal::testEqualBoolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDateStrictYear_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualDate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualEnum_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualFloat_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualInteger_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualPrimitiveExtension_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualString_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Date_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::greaterThan_Date_1__Date_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Number_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::greaterThan_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_String_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::greaterThan_String_1__String_1__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Date_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::greaterThanEqual_Date_1__Date_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Number_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::greaterThanEqual_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_String_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::greaterThanEqual_String_1__String_1__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_Boolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_Date_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_Date_1__Date_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_Number_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::lessThan::testLessThan_String_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_String_1__String_1__Boolean_1_\""),

            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_Boolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_Date_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThanEqual_Date_1__Date_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_Number_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThanEqual_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::lessThanEqual::testLessThanEqual_String_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThanEqual_String_1__String_1__Boolean_1_\""),

            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteralFromVar_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_\""),
            one("meta::pure::functions::collection::tests::filter::testFilterLiteral_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_\""),
            one("meta::pure::functions::collection::tests::filter::testLambdaAsFunctionParameter_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::filter_T_MANY__Function_1__T_MANY_\""),

            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::first_T_MANY__T_$0_1$_\""),
            one("meta::pure::functions::collection::tests::first::testFirstOnOneElement_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::first_T_MANY__T_$0_1$_\""),
            one("meta::pure::functions::collection::tests::first::testFirstSimple_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::first_T_MANY__T_$0_1$_\""),

            one("meta::pure::functions::collection::tests::isEmpty::testIsEmptyFalseMultiple_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::isEmpty_Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::isEmpty::testIsEmptyFalseSingle_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::isEmpty::testIsEmpty_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::isEmpty_Any_$0_1$__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::isEmpty::testIsNotEmptyFalse_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::isNotEmpty_Any_$0_1$__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::isEmpty::testIsNotEmptyMultiple_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::not_Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::isEmpty::testIsNotEmptySingle_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::isNotEmpty_Any_$0_1$__Boolean_1_\""),

            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "Can't find variable class for variable 'address' in the graph"),

            one("meta::pure::functions::collection::tests::range::testRangeWithStartStopEqual_Function_1__Boolean_1_", "com/fasterxml/jackson/datatype/jsr310/JavaTimeModule"),
            one("meta::pure::functions::collection::tests::range::testRangeWithStep_Function_1__Boolean_1_", "Could not initialize class org.apache.arrow.vector.util.JsonStringArrayList"),
            one("meta::pure::functions::collection::tests::range::testRangeWithVariables_Function_1__Boolean_1_", "Could not initialize class org.apache.arrow.vector.util.JsonStringArrayList"),
            one("meta::pure::functions::collection::tests::range::testRange_Function_1__Boolean_1_", "Could not initialize class org.apache.arrow.vector.util.JsonStringArrayList"),
            one("meta::pure::functions::collection::tests::range::testReverseRangeWithPositiveStep_Function_1__Boolean_1_", "Could not initialize class org.apache.arrow.vector.util.JsonStringArrayList"),
            one("meta::pure::functions::collection::tests::range::testReverseRangeWithStep_Function_1__Boolean_1_", "Could not initialize class org.apache.arrow.vector.util.JsonStringArrayList"),
            one("meta::pure::functions::collection::tests::range::testReverseRange_Function_1__Boolean_1_", "Could not initialize class org.apache.arrow.vector.util.JsonStringArrayList"),

            one("meta::pure::functions::collection::tests::size::testSizeEmpty_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::size_Any_MANY__Integer_1_\""),
            one("meta::pure::functions::collection::tests::size::testSizeOnGraphObject_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::size_Any_MANY__Integer_1_\""),
            one("meta::pure::functions::collection::tests::size::testSize_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::size_Any_MANY__Integer_1_\""),

            one("meta::pure::functions::lang::tests::compare::testBooleanCompare_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::greaterThan_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::lang::tests::compare::testCompareDecimalAndLongTypes_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::equal_Any_MANY__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::lang::tests::compare::testCompareMixedTypes_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::not_Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::lang::tests::compare::testDateCompare_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::lang::tests::compare::testFloatCompare_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::lang::tests::compare::testIntegerCompare_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::lang::tests::compare::testNumberCompare_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_\""),
            one("meta::pure::functions::lang::tests::compare::testStringCompare_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::lessThan_Number_1__Number_1__Boolean_1_\""),

            one("meta::pure::functions::lang::tests::letFn::testAssignLiteralToVariable_Function_1__Boolean_1_", "\"Match failure: LambdaFunctionObject instanceOf LambdaFunction\""),
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "\"Match failure: LambdaFunctionObject instanceOf LambdaFunction\""),
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::letFunction_String_1__T_m__T_m_\""),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::letFunction_String_1__T_m__T_m_\""),
            one("meta::pure::functions::lang::tests::letFn::testLetInsideIf_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_\""),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::multiplicity::toOne_T_MANY__T_1_\""),

            one("meta::pure::functions::math::tests::divide::testComplexDivide_Function_1__Boolean_1_", "\"\nexpected: 3.0\nactual:   0.08333333333333333\""),

            one("meta::pure::functions::math::tests::minus::testComplexMinus_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\""),
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),
            one("meta::pure::functions::math::tests::minus::testFloatMinus_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),
            one("meta::pure::functions::math::tests::minus::testLargeMinus_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\""),
            one("meta::pure::functions::math::tests::minus::testSimpleMinus_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),
            one("meta::pure::functions::math::tests::minus::testSingleExpressionMinus_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::multiplicity::toOne_T_MANY__T_1_\""),
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),

            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\"\nexpected: 6.0D\nactual:   6.0\""),
            one("meta::pure::functions::math::tests::plus::testFloatPlus_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "\"\nexpected: -1\nactual:   -9223372036854775790\""),
            one("meta::pure::functions::math::tests::plus::testPlusNumber_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::multiplicity::toOne_T_MANY__T_1_\""),

            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\"\nexpected: 353791.470D\nactual:   353791.47000000003\""),
            one("meta::pure::functions::math::tests::times::testFloatTimes_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),

            one("meta::pure::functions::string::tests::plus::testMultiPlusWithFunctionExpressions_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::string::plus_String_MANY__String_1_\""),
            one("meta::pure::functions::string::tests::plus::testMultiPlusWithPropertyExpressions_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::string::tests::plus::testMultiPlus_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::string::plus_String_MANY__String_1_\""),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::string::tests::plus::testPlus_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::string::plus_String_MANY__String_1_\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestDeephavenConnectionIntegrationLoader.extensions().getFirst())
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
