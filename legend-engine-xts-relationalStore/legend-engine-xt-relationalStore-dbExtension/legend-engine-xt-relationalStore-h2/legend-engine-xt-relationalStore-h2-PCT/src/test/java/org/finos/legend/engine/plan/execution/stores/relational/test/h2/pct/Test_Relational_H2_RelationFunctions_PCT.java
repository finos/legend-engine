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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalH2PCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_H2_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreRelationalH2PCTCodeRepositoryProvider.H2Adapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Pivot
            pack("meta::pure::functions::relation::tests::pivot", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"H2\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"H2\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"H2\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"H2\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"H2\"\"", AdapterQualifier.unsupportedFeature),

            // BUG: Column name with special characters is not properly escaped
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: CREATE LOCAL TEMPORARY TABLE", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "Error while executing: CREATE LOCAL TEMPORARY TABLE", AdapterQualifier.needsInvestigation),

            pack("meta::pure::functions::relation::tests::asOfJoin", "Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::AsOfJoin\" not implemented in SqlDialect for database type \"H2\"", AdapterQualifier.unsupportedFeature),

            // Pure to SQL translation for window function using range frame needs to be implemented
            one("meta::pure::functions::relation::tests::over::testRange_CurrentRow_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "\"Range window frame is not supported yet!\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_CurrentRow_WithMultiplePartitions_WithSingleOrderBy_Function_1__Boolean_1_", "\"Range window frame is not supported yet!\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_NFollowing_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "\"Range window frame is not supported yet!\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation)
        );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.H2).getFirst())
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
