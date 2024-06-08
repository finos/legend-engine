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

public class Test_JAVA_EssentialFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.essentialFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Add
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "\"meta::pure::functions::collection::add_T_MANY__Integer_1__T_1__T_$1_MANY$_ is not supported yet!\""),

            // Concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),

            // Contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::contains::ClassWithoutEquality\""),

            // Drop
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Exists
            one("meta::pure::functions::collection::tests::exists::testExistsInSelect_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),

            // Find (Not Supported Yet)
            pack("meta::pure::functions::collection::tests::find", "\"meta::pure::functions::collection::find_T_MANY__Function_1__T_$0_1$_ is not supported yet!\""),

            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "\"Assert failed\""),
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::fold::FO_Person\""),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::fold::FO_Person\""),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::fold::FO_Person\""),

            // ForAll
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),

            // Last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),

            // Slice
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Sort
            one("meta::pure::functions::collection::tests::sort::testMixedSortNoComparator_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortNoComparator_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortReversed_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSort_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::sort::testSortEmptySet_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Take
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),

            // Zip
            pack("meta::pure::functions::collection::tests::zip", "\"meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_ is not supported yet!\""),

            // Format
            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),
            one("meta::pure::functions::string::tests::format::testFormatInEvaluate_Function_1__Boolean_1_", "\"evaluate_Function_1__List_MANY__Any_MANY_ is prohibited!\""),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // ToString
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::Pair\""),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testComplexClassToString_Function_1__Boolean_1_", "\"\nexpected: '// Warning: Good for gin -- Sad times no tonic'\nactual:   '_pure.internal.meta.pure.functions.string.tests.toString.ClassWithComplexToString_Impl"),

            // Exp
            one("meta::pure::functions::math::tests::exp::testNumberExp_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Log
            one("meta::pure::functions::math::tests::log::testNumberLog_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Log10
            one("meta::pure::functions::math::tests::log10::testNumberLog10_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Mod
            one("meta::pure::functions::math::tests::mod::testModInEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),

            // Pow
            one("meta::pure::functions::math::tests::pow::testNumberPow_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),

            // Rem
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithFloat_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithMixedIntegersAndFloats_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithNegativeNumbers_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),
            one("meta::pure::functions::math::tests::rem::testRemInEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),
            one("meta::pure::functions::math::tests::rem::testRemWithDecimals_Function_1__Boolean_1_", "\"\nexpected: 0.14D\nactual:   0.14\""),

            // Round
            one("meta::pure::functions::math::tests::round::testDecimalRound_Function_1__Boolean_1_", "\"\nexpected: 3.14D\nactual:   3.14\""),

            // Sign
            one("meta::pure::functions::math::tests::sign::testSign_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),

            // ToDecimal
            one("meta::pure::functions::math::tests::toDecimal::testDecimalToDecimal_Function_1__Boolean_1_", "class java.lang.Double cannot be cast to class java.math.BigDecimal (java.lang.Double and java.math.BigDecimal are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::math::tests::toDecimal::testDoubleToDecimal_Function_1__Boolean_1_", "class java.lang.Double cannot be cast to class java.math.BigDecimal (java.lang.Double and java.math.BigDecimal are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::math::tests::toDecimal::testIntToDecimal_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.math.BigDecimal (java.lang.Long and java.math.BigDecimal are in module java.base of loader 'bootstrap')"),

            // ArcCos
            one("meta::pure::functions::math::tests::trigonometry::testArcCosineEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // ArcSine
            one("meta::pure::functions::math::tests::trigonometry::testArcSineEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // ArcTangent2
            one("meta::pure::functions::math::tests::trigonometry::testArcTangent2Eval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),

            // ArcTangent
            one("meta::pure::functions::math::tests::trigonometry::testArcTangentEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // CoTangent
            one("meta::pure::functions::math::tests::trigonometry::testCoTangentEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Cosine
            one("meta::pure::functions::math::tests::trigonometry::testCosEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // CubeRoot
            one("meta::pure::functions::math::tests::testCubeRootEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Sine
            one("meta::pure::functions::math::tests::trigonometry::testSineEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // SquareRoot
            one("meta::pure::functions::math::tests::testSquareRootEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Tangent
            one("meta::pure::functions::math::tests::trigonometry::testTangentEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"")
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
