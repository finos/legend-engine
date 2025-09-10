// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.athena.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalAthenaPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Athena_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreRelationalAthenaPCTCodeRepositoryProvider.athenaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //AsOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support AsOfJoin!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support AsOfJoin!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support AsOfJoin!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support AsOfJoin!\""),

            //Composition
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_indexOf_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_slice_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),

            //Extend Variant
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"Match failure: FoldRelationalLambdaObject instanceOf FoldRelationalLambda\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"Match failure: MapRelationalLambdaObject instanceOf MapRelationalLambda\""),

            //Filter
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"Match failure: FilterRelationalLambdaObject instanceOf FilterRelationalLambda\""),

            //Lateral
            one("meta::pure::functions::relation::tests::lateral::testLateralJoinAreInnerJoins_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::LateralJoin\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Chained_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::LateralJoin\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::LateralJoin\" not implemented in SqlDialect for database type \"Athena\"\""),

            //Pivot
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Dynamic_Aggregation_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Dynamic_Aggregation_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Dialect translation for node of type \"meta::external::query::sql::metamodel::extension::PivotedRelation\" not implemented in SqlDialect for database type \"Athena\"\""),

            //Select
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "\"[unsupported-api] special chars are not supported in table/column names\""),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "\"[unsupported-api] special chars are not supported in table/column names\""),

            //Variant Flatten
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Function_1__Boolean_1_", "\"Match failure: DataTypeInfoObject instanceOf DataTypeInfo\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Scalar_Function_1__Boolean_1_", "\"Match failure: SemiStructuredArrayFlattenRelationObject instanceOf SemiStructuredArrayFlattenRelation\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Array_Function_1__Boolean_1_", "\"Match failure: SemiStructuredArrayFlattenRelationObject instanceOf SemiStructuredArrayFlattenRelation\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Map_Function_1__Boolean_1_", "\"Match failure: SemiStructuredArrayFlattenRelationObject instanceOf SemiStructuredArrayFlattenRelation\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Navigation_Function_1__Boolean_1_", "\"Match failure: SemiStructuredArrayFlattenRelationObject instanceOf SemiStructuredArrayFlattenRelation\"")

    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Athena).getFirst())
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
