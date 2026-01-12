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
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;


public class Test_Relational_Oracle_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreRelationalOraclePCTCodeRepositoryProvider.oracleAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            // Pivot
            pack("meta::pure::functions::relation::tests::pivot", "\"pivot is not supported\""),

            //Extend
            one("meta::pure::functions::relation::tests::composition::testExtendWindowFilter_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   2,1,B,16\n   6,1,F,16\n   8,1,H,16\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10,0,J,10\n   2,1,B,16\n   6,1,F,16\n   8,1,H,16\n   1,2,A,6\n   5,2,E,6\n   3,3,C,10\n   7,3,G,10\n   4,4,D,4\n   9,5,I,9\n#'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::composition::testGroupByFilterExtendFilter_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,rank,sumId,sumRank\n   A,1,9,3\n   A,2,8,3\n#'\nactual:   '#TDS\n   grp,rank,sumId,sumRank\n   A,1,9,3\n   D,1,10,1\n   A,2,8,3\n   B,2,8,2\n#'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),

            // Needs support for asOf Join
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\"", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Oracle\"", AdapterQualifier.unsupportedFeature),

            // Lateral
            pack("meta::pure::functions::relation::tests::lateral", "\"[unsupported-api] Lateral operation not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),

            //Variant
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, true]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, true]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_indexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_slice_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_contains_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_contains' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_distinct_removeDuplicates_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_projectModelProperty_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //Order by SQL Issue
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderASCUnboundedWindow_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_MultipleExpressions_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndUnboundedWindow_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30485: missing ORDER BY expression in the window specification\n\nhttps://docs.oracle.com/error-help/db/ora-30485/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30487: ORDER BY not allowed here\n\nhttps://docs.oracle.com/error-help/db/ora-30487/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithSinglePartition_WithoutOrderBy_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30485: missing ORDER BY expression in the window specification\n\nhttps://docs.oracle.com/error-help/db/ora-30485/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithMultiplePartitions_WithoutOrderBy_Function_1__Boolean_1_", "java.sql.SQLException: ORA-30485: missing ORDER BY expression in the window specification\n\nhttps://docs.oracle.com/error-help/db/ora-30485/", AdapterQualifier.needsInvestigation),

            //Can't make a temp table
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_", AdapterQualifier.needsInvestigation),

            //String to boolean conversion
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_MultipleExpressions_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),

            //Flatten
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: Oracle\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Extend_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: Oracle\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Scalar_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Array_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Map_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Navigation_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Oracle\"", AdapterQualifier.unsupportedFeature)
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
