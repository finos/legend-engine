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

package org.finos.legend.engine.plan.execution.stores.relational.test.duckdb.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_DuckDB_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.duckDBAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // TODO: DuckDB generates the row in random order, sorting is needed for the assertion
            // but that relies on support for casting (i.e. ->cast(..)->sort(...)
            one("meta::pure::functions::relation::tests::pivot::testSimplePivotBySingleMultiple_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   city,country,2000__|__sum,2000__|__count,2011__|__sum,2011__|__count,2012__|__sum,2012__|__count\n   LND,UK,null,null,3000,1,null,null\n   NYC,USA,15000,2,5000,1,15200,2\n   SAN,USA,2000,1,2600,2,null,null\n#'\nactual:   '#TDS\n   city,country,2000__|__sum,2000__|__count,2011__|__sum,2011__|__count,2012__|__sum,2012__|__count\n   NYC,USA,15000,2,5000,1,15200,2\n   SAN,USA,2000,1,2600,2,null,null\n   LND,UK,null,null,3000,1,null,null\n#'\""),
            one("meta::pure::functions::relation::tests::pivot::testSimplePivotBySingleSingle_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   city,country,2000__|__newCol,2011__|__newCol,2012__|__newCol\n   LND,UK,null,3000,null\n   NYC,USA,15000,5000,15200\n   SAN,USA,2000,2600,null\n#'\nactual:   '#TDS\n   city,country,2000__|__newCol,2011__|__newCol,2012__|__newCol\n   SAN,USA,2000,2600,null\n   LND,UK,null,3000,null\n   NYC,USA,15000,5000,15200\n#'\""),

            // temporarily fix, need to investigate further, there is potentially some problem with the router
            // and cast(@Relation<(...)>), the generic type is being erased somehow, so we cannot reason about the types of relation columns
            one("meta::pure::functions::relation::tests::pivot::testSimplePivotChained_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.DuckDB).getFirst())
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
