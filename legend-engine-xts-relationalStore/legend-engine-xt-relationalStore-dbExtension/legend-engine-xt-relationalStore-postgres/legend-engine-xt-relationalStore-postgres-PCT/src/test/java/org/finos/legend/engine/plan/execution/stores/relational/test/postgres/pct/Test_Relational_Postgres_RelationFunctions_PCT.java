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

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.pct;

import junit.framework.Test;
import org.checkerframework.checker.units.qual.A;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalPostgresPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;


public class Test_Relational_Postgres_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreRelationalPostgresPCTCodeRepositoryProvider.postgresAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Pivot
            pack("meta::pure::functions::relation::tests::pivot", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),

            // BUG: Column name with special characters is not properly escaped
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.", AdapterQualifier.needsInvestigation),

            // Postgres doesn't support asOf Join (May want to compensate with an OLAP equivalent if required
            pack("meta::pure::functions::relation::tests::asOfJoin", "\"AsOfJoins are not supported in the generic generator!\"", AdapterQualifier.unsupportedFeature),

            // Null values not supported in source TDS
            one("meta::pure::functions::relation::tests::over::testRange_ExplicitOffsets_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRange_ExplicitOffsets_WithNullValues_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithNullValues_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            // Postgres does not support range frame with explicit offsets on either side
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE PRECEDING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_CurrentRow_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_NFollowing_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: RANGE FOLLOWING is only supported with UNBOUNDED", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Postgres\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testGroupByFilterExtendFilter_Function_1__Boolean_1_", "\"QUALIFY grammar is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendWindowFilter_Function_1__Boolean_1_", "\"QUALIFY grammar is not supported\"", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Postgres\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Postgres\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),


            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Postgres\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature),

            // lateral
            pack("meta::pure::functions::relation::tests::lateral", "[unsupported-api] Lateral operation not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature),

            // flatten
            pack("meta::pure::functions::relation::variant::tests::flatten", "Datatype to SQL text not supported for Database Type: Postgres", AdapterQualifier.unsupportedFeature)
        );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Postgres).getFirst())
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
