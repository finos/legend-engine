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
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_RelationFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Extend
            one("meta::pure::functions::relation::tests::composition::testExtendFilter_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),

            // Concatenate
            pack("meta::pure::functions::relation::tests::concatenate", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::concatenate_Relation_1__Relation_1__Relation_1_ is not supported yet!\""),

            //Drop
            pack("meta::pure::functions::relation::tests::drop", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::drop_Relation_1__Integer_1__Relation_1_ is not supported yet!\""),

            // Extend
            pack("meta::pure::functions::relation::tests::extend", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::extend_Relation_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::extend_Relation_1__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Filter
            pack("meta::pure::functions::relation::tests::filter", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),

            // GroupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBySingleSingle_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupByMultipleMultiple_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupByMultipleSingle_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBySingleMultiple_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Join
            one("meta::pure::functions::relation::tests::join::testSimpleJoinShared_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::join_Relation_1__Relation_1__JoinKind_1__Function_1__Relation_1_ is not supported yet!\""),

            // Limit
            one("meta::pure::functions::relation::tests::limit::testSimpleLimitShared_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::limit_Relation_1__Integer_1__Relation_1_ is not supported yet!\""),

            // Project
            pack("meta::pure::functions::relation::tests::project", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::project_C_MANY__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Rename
            one("meta::pure::functions::relation::tests::rename::testSimpleRenameShared_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::rename_Relation_1__ColSpec_1__ColSpec_1__Relation_1_ is not supported yet!\""),

            // Select
            one("meta::pure::functions::relation::tests::select::testMultiColsSelectShared_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::select_Relation_1__ColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::select::testSingleColSelectShared_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::select_Relation_1__ColSpec_1__Relation_1_ is not supported yet!\""),

            // Size
            pack("meta::pure::functions::relation::tests::size", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::size_Relation_1__Integer_1_ is not supported yet!\""),

            // Slice
            one("meta::pure::functions::relation::tests::slice::testSimpleSliceShared_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::slice_Relation_1__Integer_1__Integer_1__Relation_1_ is not supported yet!\""),

            // Sort
            one("meta::pure::functions::relation::tests::sort::testSimpleSortShared_Function_1__Boolean_1_", "Assert failure at (resource:/core_external_language_java/generation/expressionGeneration.pure line:235 column:10), \"meta::pure::functions::relation::sort_Relation_1__SortInfo_MANY__Relation_1_ is not supported yet!\"")



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
