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
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreUnclassifiedFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_StandardFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationPopulation_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationSample_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),

            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVariancePopulation_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVarianceSample_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),

            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByWavg_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByMultipleWavg_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),

            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::in::Firm\""),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "error: incompatible types: double cannot be converted to java.util.List<java.lang.Number>"),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "error: incompatible types: long cannot be converted to java.util.List<java.lang.Number>"),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "error: incompatible types: java.math.BigDecimal cannot be converted to java.util.List<java.lang.Number>"),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),

            // StdDev
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationPopulation_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationSample_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),

            // Variance
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVariancePopulation_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVarianceSample_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),

            // Sum
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Window_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\"")
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
