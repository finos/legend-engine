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

            pack("meta::pure::functions::relation::tests::asOfJoin", "H2 SQL Dialect does not support AsOfJoin!", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda\"", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"Match failure: FoldRelationalLambdaObject instanceOf FoldRelationalLambda\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda\"", AdapterQualifier.needsImplementation),
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
