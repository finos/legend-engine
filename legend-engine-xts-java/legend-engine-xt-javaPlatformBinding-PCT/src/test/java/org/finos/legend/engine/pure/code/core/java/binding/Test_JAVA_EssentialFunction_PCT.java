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
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
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
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "\"meta::pure::functions::collection::add_T_MANY__Integer_1__T_1__T_$1_MANY$_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),

            // Concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),

            // Contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "Can't find a match for function 'contains(ClassWithoutEquality[*],ClassWithoutEquality[1],ConcreteFunctionDefinition<{ClassWithoutEquality[1], ClassWithoutEquality[1]->Boolean[1]}>[1])'.", AdapterQualifier.unsupportedFeature),

            // Drop
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "Generics not supported (function: meta::pure::functions::collection::list_U_MANY__List_1_)", AdapterQualifier.needsImplementation),

            // Exists
            one("meta::pure::functions::collection::tests::exists::testExistsInSelect_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\"", AdapterQualifier.needsInvestigation),

            // Find (Not Supported Yet)
            pack("meta::pure::functions::collection::tests::find", "Can't find variable class for variable '", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::find::testFindUsingVarForFunction_Function_1__Boolean_1_", "Error in 'test::testFunction': Function does not exist 'find(CO_Person[3],LambdaFunction<{CO_Person[1]->Boolean[1]}>[1])'", AdapterQualifier.unsupportedFeature),

            // Init (Not Supported Yet)
            pack("meta::pure::functions::collection::tests::init", "Function does not exist 'init(", AdapterQualifier.unsupportedFeature),


            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "Function does not exist 'copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 4 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "Function does not exist 'copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "Function does not exist 'copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.unsupportedFeature),

            // ForAll
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 1 error compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:", AdapterQualifier.needsInvestigation),

            // Head
            one("meta::pure::functions::collection::tests::head::testHeadComplex_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\"", AdapterQualifier.needsInvestigation),

            // Last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 2 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:", AdapterQualifier.needsInvestigation),

            // Slice
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "Generics not supported (function: meta::pure::functions::collection::list_U_MANY__List_1_)", AdapterQualifier.needsImplementation),

            // Take
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "Generics not supported (function: meta::pure::functions::collection::list_U_MANY__List_1_)", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "Failed in node: root", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "Failed in node: root", AdapterQualifier.needsInvestigation),

            // Zip
            pack("meta::pure::functions::collection::tests::zip", "\"meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),

            // Format
            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatInEvaluate_Function_1__Boolean_1_", "Function does not exist 'evaluate(NativeFunction<{String[1], Any[*]->String[1]}>[1],List<T>[2])'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::Pair\"", AdapterQualifier.needsInvestigation),

            // ToString
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 3 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 4 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::Pair\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::Pair\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testComplexClassToString_Function_1__Boolean_1_", "\"\nexpected: '// Warning: Good for gin -- Sad times no tonic'\nactual:   '_pure.internal.meta.pure.functions.string.tests.toString.ClassWithComplexToString_Impl", AdapterQualifier.needsInvestigation),

            // Exp
            one("meta::pure::functions::math::tests::exp::testNumberExp_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // Log
            one("meta::pure::functions::math::tests::log::testNumberLog_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // Log10
            one("meta::pure::functions::math::tests::log10::testNumberLog10_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // Mod
            one("meta::pure::functions::math::tests::mod::testModInEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // Pow
            one("meta::pure::functions::math::tests::pow::testNumberPow_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // Rem
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithFloat_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithMixedIntegersAndFloats_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithNegativeNumbers_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::rem::testRemInEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::rem::testRemWithDecimals_Function_1__Boolean_1_", "\"\nexpected: 0.14D\nactual:   0.14\"", AdapterQualifier.needsInvestigation),

            // Sign
            one("meta::pure::functions::math::tests::sign::testSign_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 1 error compiling /_pure/plan/root/Execute.java", AdapterQualifier.needsInvestigation),

            // ArcCos
            one("meta::pure::functions::math::tests::trigonometry::testArcCosineEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // ArcSine
            one("meta::pure::functions::math::tests::trigonometry::testArcSineEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // ArcTangent2
            one("meta::pure::functions::math::tests::trigonometry::testArcTangent2Eval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__U_p__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // ArcTangent
            one("meta::pure::functions::math::tests::trigonometry::testArcTangentEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // CoTangent
            one("meta::pure::functions::math::tests::trigonometry::testCoTangentEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // Cosine
            one("meta::pure::functions::math::tests::trigonometry::testCosEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // CubeRoot
            one("meta::pure::functions::math::tests::testCubeRootEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // String
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimal_Function_1__Boolean_1_", "\"\nexpected: 3.1415900000D\nactual:   3.14159D\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::parseDecimal::testParseZero_Function_1__Boolean_1_", "\"\nexpected: 0.000D\nactual:   0.0D\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::reverse::testReverseString_Function_1__Boolean_1_", "\"meta::pure::functions::string::reverseString_String_1__String_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::trim::testLTrim_Function_1__Boolean_1_", "\"Type not found for method ltrim from String\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::trim::testRTrim_Function_1__Boolean_1_", "\"Type not found for method rtrim from String\"", AdapterQualifier.needsInvestigation),

            // Sine
            one("meta::pure::functions::math::tests::trigonometry::testSineEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // SquareRoot
            one("meta::pure::functions::math::tests::testSquareRootEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // Tangent
            one("meta::pure::functions::math::tests::trigonometry::testTangentEval_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionExplicit_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::metamodel::function::NativeFunction\"", AdapterQualifier.needsInvestigation),

            // Date
            one("meta::pure::functions::date::tests::testAdjustReflectiveEvaluation_Function_1__Boolean_1_", "Can't find a match for function 'eval(NativeFunction<{Date[1], Integer[1], DurationUnit[1]->Date[1]}>[1],StrictDate[1],Integer[1],DurationUnit[1])'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasMonthReflect_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testYeaReflect_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\"", AdapterQualifier.unsupportedFeature),

            // If
            one("meta::pure::functions::lang::tests::if::testMultiIf_Function_1__Boolean_1_", "\"Generics not supported (function: meta::pure::functions::lang::if_Pair_MANY__Function_1__T_m_)\""),

            // Match
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParam_Function_1__Boolean_1_", "meta::pure::functions::lang::match_Any_MANY__Function_$1_MANY$__P_o__T_m_ is not supported yet!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParamsAndFunctionsAsParam_Function_1__Boolean_1_", "meta::pure::functions::lang::match_Any_MANY__Function_$1_MANY$__P_o__T_m_ is not supported yet!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParam_Function_1__Boolean_1_", "Can't find a match for function 'match(Integer[1],LambdaFunction<{Nil[1]->Integer[1..3]}>[*])'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParamManyMatch_Function_1__Boolean_1_", "Can't find a match for function 'match(Integer[1],LambdaFunction<{Nil[*]->Any[*]}>[*])'", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithMixedReturnType_Function_1__Boolean_1_", "Function does not exist 'deactivate(Any[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::match::testMatch_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 8 errors compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:", AdapterQualifier.needsInvestigation),

            //variant
            one("meta::pure::functions::collection::tests::fold::testFold_FromVariantAsPrimitive_Function_1__Boolean_1_", "\"meta::pure::functions::variant::convert::toMany_Variant_$0_1$__T_$0_1$__T_MANY_ is not supported yet!", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFold_FromVariant_Function_1__Boolean_1_", "\"meta::pure::functions::variant::convert::toMany_Variant_$0_1$__T_$0_1$__T_MANY_ is not supported yet!", AdapterQualifier.needsImplementation)
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
