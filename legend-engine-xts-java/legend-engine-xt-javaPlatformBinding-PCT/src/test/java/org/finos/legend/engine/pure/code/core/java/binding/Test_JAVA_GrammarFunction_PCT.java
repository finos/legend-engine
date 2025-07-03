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

public class Test_JAVA_GrammarFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.grammarFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Eq
            one("meta::pure::functions::boolean::tests::equality::eq::testEqNonPrimitive_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::SideClass\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::eq::testEqVarIdentity_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::BottomClass\"", AdapterQualifier.needsInvestigation),
            // Equal
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualNonPrimitive_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::SideClass\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::equality::equal::testEqualVarIdentity_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::boolean::tests::equalitymodel::BottomClass\"", AdapterQualifier.needsInvestigation),

            // GreaterThan
            one("meta::pure::functions::boolean::tests::inequalities::greaterThan::testGreaterThan_Boolean_Function_1__Boolean_1_", "\"Assert failed\"", AdapterQualifier.needsInvestigation),

            // GreaterThanEqual
            one("meta::pure::functions::boolean::tests::inequalities::greaterThanEqual::testGreaterThanEqual_Boolean_Function_1__Boolean_1_", "\"Assert failed\"", AdapterQualifier.needsInvestigation),

            // Filter
            one("meta::pure::functions::collection::tests::filter::testFilterInstance_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Person\"", AdapterQualifier.needsInvestigation),

            // First
            one("meta::pure::functions::collection::tests::first::testFirstComplex_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::model::CO_Firm\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::first::testFirstOnEmptySet_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 2 errors compiling /_pure/plan/root/Execute.java", AdapterQualifier.needsInvestigation),

            // Let
            one("meta::pure::functions::lang::tests::letFn::testAssignNewInstance_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::lang::tests::model::LA_Person\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 2 errors compiling /_pure/plan/root/n1/Execute.java\n/_pure/plan/root/n1/Execute.java", AdapterQualifier.needsInvestigation),

            // Map
            one("meta::pure::functions::collection::tests::map::testMapInstance_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::map::model::M_Person\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToMany_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::map::model::M_Person\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromManyToOne_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::map::model::M_Person\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::map::testMapRelationshipFromOneToOne_Function_1__Boolean_1_", "Error during dynamic reactivation: Error dynamically evaluating value specification (from /platform/pure/grammar/functions/collection/iteration/map.pure:45cc92-98); error compiling generated Java code:", AdapterQualifier.needsInvestigation),

            // let
            one("meta::pure::functions::lang::tests::letFn::testLetAsLastStatement_Function_1__Boolean_1_", "Can't resolve the builder for function 'letAsLastStatement'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetChainedWithAnotherFunction_Function_1__Boolean_1_", "Can't resolve the builder for function 'letChainedWithAnotherFunction'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::letFn::testLetWithParam_Function_1__Boolean_1_", "Can't resolve the builder for function 'letWithParam'", AdapterQualifier.unsupportedFeature),

            // Minus
            one("meta::pure::functions::math::tests::minus::testDecimalMinus_Function_1__Boolean_1_", "\"\nexpected: -4.0D\nactual:   -4.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testSingleMinus_Function_1__Boolean_1_", "org.finos.legend.engine.shared.javaCompiler.JavaCompileException: 1 error compiling /_pure/plan/root/Execute.java\n/_pure/plan/root/Execute.java:11: error: unexpected type\n            return --1L;\n                     ^\n  required: variable\n  found:    value\n\n/_pure/plan/root/Execute.java\n0001 package _pure.plan.root;\n0002 \n0003 import org.finos.legend.engine.plan.dependencies.store.shared.IExecutionNodeContext;\n0004 \n0005 public class Execute\n0006 {\n0007     public static long execute(IExecutionNodeContext context)\n0008     {\n0009         try\n0010         {\n0011             return --1L;\n0012         }\n0013         catch (Exception e)\n0014         {\n0015             throw new RuntimeException(\"Failed in node: root\", e);\n0016         }\n0017     }", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::minus::testSingleMinusType_Function_1__Boolean_1_", "\"meta::pure::functions::meta::genericType_Any_MANY__GenericType_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),

            // Plus
            one("meta::pure::functions::math::tests::plus::testDecimalPlus_Function_1__Boolean_1_", "\"\nexpected: 6.0D\nactual:   6.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::plus::testLargePlus_Function_1__Boolean_1_", "\"\nexpected: -1\nactual:   -9223372036854775790\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::plus::testSinglePlusType_Function_1__Boolean_1_", "\"meta::pure::functions::meta::genericType_Any_MANY__GenericType_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),

            // Times
            one("meta::pure::functions::math::tests::times::testDecimalTimes_Function_1__Boolean_1_", "\"\nexpected: 353791.470D\nactual:   353791.47\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "\"\nexpected: -1\nactual:   -2\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::times::testLargeTimes_Function_1__Boolean_1_", "\"\nexpected: -1\nactual:   -2\"", AdapterQualifier.needsInvestigation),

            // variant
            one("meta::pure::functions::collection::tests::filter::testFilter_FromVariantAsPrimitive_Function_1__Boolean_1_", "meta::pure::functions::variant::convert::toVariant_Any_MANY__Variant_1_ is not supported yet!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::filter::testFilter_FromVariant_Function_1__Boolean_1_", "meta::pure::functions::variant::convert::toVariant_Any_MANY__Variant_1_ is not supported yet!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::map::testMap_FromVariantAsPrimitive_Function_1__Boolean_1_", "meta::pure::functions::variant::convert::toVariant_Any_MANY__Variant_1_ is not supported yet!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::map::testMap_FromVariant_Function_1__Boolean_1_", "meta::pure::functions::variant::convert::toVariant_Any_MANY__Variant_1_ is not supported yet!", AdapterQualifier.unsupportedFeature),
            
            // String plus
            one("meta::pure::functions::string::tests::plus::testPlusInIterate_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::string::tests::plus::model::P_Person\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::plus::testPlusInCollect_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::string::tests::plus::model::P_Person\"", AdapterQualifier.needsInvestigation)

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
