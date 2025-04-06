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
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "Can't find a match for function 'contains(?)'"),

            // Drop
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "Can't resolve the builder for function 'list'"),

            // Exists
            one("meta::pure::functions::collection::tests::exists::testExistsInSelect_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),

            // Find (Not Supported Yet)
            pack("meta::pure::functions::collection::tests::find", "Can't resolve the builder for function 'find'"),

            // Init (Not Supported Yet)
            pack("meta::pure::functions::collection::tests::init", "Can't resolve the builder for function 'init'"),


            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "Can't resolve the builder for function 'copy'"),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 4 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:"),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "Can't resolve the builder for function 'copy'"),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "Can't resolve the builder for function 'copy'"),

            // ForAll
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 1 error compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:"),

            // Head
            one("meta::pure::functions::collection::tests::head::testHeadComplex_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\""),

            // Last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 2 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:"),

            // Slice
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "Can't resolve the builder for function 'list'"),

            // Take
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "Can't resolve the builder for function 'list'"),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "Failed in node: root"),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "Failed in node: root"),

            // Zip
            pack("meta::pure::functions::collection::tests::zip", "\"meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_ is not supported yet!\""),

            // Format
            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\""),
            one("meta::pure::functions::string::tests::format::testFormatInEvaluate_Function_1__Boolean_1_", "Can't resolve the builder for function 'evaluate'"),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::Pair\""),

            // ToString
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 3 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java"),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 4 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java"),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::Pair\""),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::Pair\""),
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

            // Sign
            one("meta::pure::functions::math::tests::sign::testSign_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 1 error compiling /_pure/plan/root/Execute.java"),

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

            // String
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimal_Function_1__Boolean_1_", "\"\nexpected: 3.1415900000D\nactual:   3.14159D\""),
            one("meta::pure::functions::string::tests::parseDecimal::testParseZero_Function_1__Boolean_1_", "\"\nexpected: 0.000D\nactual:   0.0D\""),
            one("meta::pure::functions::string::tests::reverse::testReverseString_Function_1__Boolean_1_", "\"meta::pure::functions::string::reverseString_String_1__String_1_ is not supported yet!\""),
            one("meta::pure::functions::string::tests::trim::testLTrim_Function_1__Boolean_1_", "\"Type not found for method ltrim from String\""),
            one("meta::pure::functions::string::tests::trim::testRTrim_Function_1__Boolean_1_", "\"Type not found for method rtrim from String\""),

            // Sine
            one("meta::pure::functions::math::tests::trigonometry::testSineEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // SquareRoot
            one("meta::pure::functions::math::tests::testSquareRootEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Tangent
            one("meta::pure::functions::math::tests::trigonometry::testTangentEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // IsNonPrimitive
            one("meta::pure::functions::boolean::tests::testIsNonPrimitive_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::SideClass\""),

            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionExplicit_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::metamodel::function::NativeFunction\""),

            // Date
            one("meta::pure::functions::date::tests::testAdjustReflectiveEvaluation_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't find a match for function 'eval(?)'"),
            one("meta::pure::functions::date::tests::testHasMonthReflect_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),
            one("meta::pure::functions::date::tests::testYeaReflect_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // If
            one("meta::pure::functions::lang::tests::if::testMultiIf_Function_1__Boolean_1_", "Can't find a match for function 'if(?)'"),

            // Match
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParam_Function_1__Boolean_1_", "Can't find a match for function 'match(?)'"),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParamsAndFunctionsAsParam_Function_1__Boolean_1_", "Can't find a match for function 'match(?)'"),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParam_Function_1__Boolean_1_", "Can't find a match for function 'match(Integer[1],LambdaFunction<{Nil[1]->Integer[1..3]}>[*])'"),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParamManyMatch_Function_1__Boolean_1_", "Can't find a match for function 'match(Integer[1],LambdaFunction<{Nil[*]->Any[*]}>[*])'"),
            one("meta::pure::functions::lang::tests::match::testMatchWithMixedReturnType_Function_1__Boolean_1_", "Can't resolve the builder for function 'deactivate'"),
            one("meta::pure::functions::lang::tests::match::testMatch_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 8 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:")
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
