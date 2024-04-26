// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContext;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.query.sql.providers.core.*;
import org.finos.legend.engine.query.sql.providers.shared.AbstractTestLegendStoreSQLSourceProvider;
import org.finos.legend.engine.query.sql.providers.shared.SQLSourceProviderTestUtils;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateLoader;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateWrapper;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectResolvedContext;
import org.finos.legend.engine.query.sql.providers.shared.utils.SQLProviderUtils;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.finos.legend.engine.query.sql.providers.shared.SQLSourceProviderTestUtils.loadPureModelContextFromResource;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestRelationalStoreSQLSourceProvider extends AbstractTestLegendStoreSQLSourceProvider
{
    private static final String CONNECTION_NAME = "simple::store::DB::H2Connection";

    @Mock
    private ProjectCoordinateLoader projectCoordinateLoader;

    private RelationalStoreSQLSourceProvider provider;

    @Before
    public void setup()
    {
        provider = new RelationalStoreSQLSourceProvider(projectCoordinateLoader);
    }

    @Override
    protected SQLSourceProvider getProvider()
    {
        return provider;
    }

    @Override
    protected ProjectCoordinateLoader getProjectCoordinateLoader()
    {
        return projectCoordinateLoader;
    }

    @Test
    public void testType()
    {
        Assert.assertEquals("relationalStore", provider.getType());
    }

    @Test
    public void testMissingSchema()
    {
        TableSource tableSource = new TableSource("relationalStore", FastList.newListWith(
                new TableSourceArgument("store", null, "simple::store::DBForSQL"),
                new TableSourceArgument("coordinates", null, "group:artifact:version"),
                new TableSourceArgument("connection", null, CONNECTION_NAME)));

        testError(tableSource, ProjectCoordinateWrapper.coordinates("group:artifact:version"), "'schema' parameter is required");
    }

    @Test
    public void testMissingTable()
    {
        TableSource tableSource = new TableSource("relationalStore", FastList.newListWith(
                new TableSourceArgument("store", null, "simple::store::DBForSQL"),
                new TableSourceArgument("schema", null, "nonexistent"),
                new TableSourceArgument("connection", null, CONNECTION_NAME),
                new TableSourceArgument("coordinates", null, "group:artifact:version")));

        testError(tableSource, ProjectCoordinateWrapper.coordinates("group:artifact:version"), "'table' parameter is required");
    }

    @Test
    public void testDatabaseFoundSchemaNotFound()
    {
        testNotFound("nonexistent", "nonexistent", "No element found for 'schema'");
    }

    @Test
    public void testDatabaseFoundSchemaFoundTableNotFound()
    {
        testNotFound("DBSchema", "nonexistent", "No element found for 'table'");
    }

    @Test
    public void testSingleFromCoordinates()
    {
        testSuccess(
                ProjectCoordinateWrapper.coordinates("group:artifact:version"),
                new PureModelContextPointer(),
                FastList.newListWith(new TableSourceArgument("coordinates", null, "group:artifact:version")),
                FastList.newListWith(new SQLSourceArgument("coordinates", null, "group:artifact:version")));

    }

    @Test
    public void testSingleFromProjectWorkspace()
    {
        testSuccess(
                ProjectCoordinateWrapper.workspace("proj1", "ws1"),
                new PureModelContextPointer(),
                FastList.newListWith(
                        new TableSourceArgument("project", null, "proj1"),
                        new TableSourceArgument("workspace", null, "ws1")),
                FastList.newListWith(
                        new SQLSourceArgument("project", null, "proj1"),
                        new SQLSourceArgument("workspace", null, "ws1")
                )
        );
    }

    @Test
    public void testSingleFromProjectGroupWorkspace()
    {
        testSuccess(
                ProjectCoordinateWrapper.groupWorkspace("proj1", "ws1"),
                new PureModelContextPointer(),
                FastList.newListWith(
                        new TableSourceArgument("project", null, "proj1"),
                        new TableSourceArgument("groupWorkspace", null, "ws1")),
                FastList.newListWith(
                        new SQLSourceArgument("project", null, "proj1"),
                        new SQLSourceArgument("groupWorkspace", null, "ws1")
                )
        );
    }

    private void testNotFound(String schema, String table, String error)
    {
        PureModelContextData pmcd = loadPureModelContextFromResource("pmcd.pure", this.getClass());
        when(projectCoordinateLoader.resolve(eq(ProjectCoordinateWrapper.coordinates("group:artifact:version")), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd));

        TableSource tableSource = new TableSource("relationalStore", FastList.newListWith(
                new TableSourceArgument("store", null, "simple::store::DBForSQL"),
                new TableSourceArgument("connection", null, CONNECTION_NAME),
                new TableSourceArgument("schema", null, schema),
                new TableSourceArgument("table", null, table),
                new TableSourceArgument("coordinates", null, "group:artifact:version")));

        testError(tableSource, error);
    }

    private void testError(TableSource tableSource, ProjectCoordinateWrapper projectCoordinateWrapper, String error)
    {
        PureModelContextData pmcd = loadPureModelContextFromResource("pmcd.pure", this.getClass());
        when(projectCoordinateLoader.resolve(eq(projectCoordinateWrapper), any())).thenReturn(new ProjectResolvedContext(pmcd, pmcd));

        testError(tableSource, error);
    }

    private void testSuccess(ProjectCoordinateWrapper projectCoordinateWrapper, PureModelContext expectedContext, List<TableSourceArgument> tableSourceKeys, List<SQLSourceArgument> sourceKeys)
    {
        PureModelContextData pmcd = loadPureModelContextFromResource("pmcd.pure", this.getClass());
        when(projectCoordinateLoader.resolve(eq(projectCoordinateWrapper), any())).thenReturn(new ProjectResolvedContext(expectedContext, pmcd));

        String databaseName = "simple::store::DBForSQL";
        String schemaName = "DBSchema";
        String tableName = "FIRM_TABLE";

        TableSource tablesource = new TableSource("relationalStore", FastList.newListWith(
                new TableSourceArgument("store", null, databaseName),
                new TableSourceArgument("schema", null, schemaName),
                new TableSourceArgument("table", null, tableName),
                new TableSourceArgument("connection", null, CONNECTION_NAME)).withAll(tableSourceKeys)
        );

        List<SQLSourceArgument> keys = FastList.newListWith(
                new SQLSourceArgument("store", null, databaseName),
                new SQLSourceArgument("connection", null, CONNECTION_NAME))
                .withAll(sourceKeys)
                .with(new SQLSourceArgument("schema", null, schemaName))
                .with(new SQLSourceArgument("table", null, tableName));

        SQLSourceResolvedContext result = provider.resolve(FastList.newListWith(tablesource), null, IdentityFactoryProvider.getInstance().getAnonymousIdentity());

        Lambda lambda = SQLProviderUtils.tableToTDS(databaseName, schemaName, tableName);

        ConnectionPointer connectionPtr = new ConnectionPointer();
        connectionPtr.connection = CONNECTION_NAME;

        EngineRuntime runtime = SQLProviderUtils.createRuntime(CONNECTION_NAME, databaseName);

        SQLSource expected = new SQLSource("relationalStore", lambda, null, runtime, null, null, keys);

        //ASSERT
        Assert.assertEquals(FastList.newListWith(expectedContext), result.getPureModelContexts());
        Assert.assertEquals(1, result.getSources().size());

        SQLSourceProviderTestUtils.assertLogicalEquality(expected, result.getSources().get(0));
    }
}