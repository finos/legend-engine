// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.protocol.pure.v1.extension.TestConnectionBuildParameters;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.data.RelationalCSVTable;
import org.junit.Assert;
import org.junit.Test;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TestRelationalConnectionFactoryDuckDB
{
    private RelationalCSVData createSimpleRelationalCSVData()
    {
        RelationalCSVData data = new RelationalCSVData();
        RelationalCSVTable table = new RelationalCSVTable();
        table.schema = "default";
        table.table = "PersonTable";
        table.values = "id,name,age\n1,Alice,30\n2,Bob,25\n";
        data.tables = Collections.singletonList(table);
        return data;
    }

    @Test
    public void testBuildRelationalTestConnection_DefaultH2()
    {
        RelationalConnectionFactory factory = new RelationalConnectionFactory();
        RelationalCSVData data = createSimpleRelationalCSVData();

        Optional<Pair<Connection, List<Closeable>>> result = factory.buildRelationalTestConnection("myDB", data, TestConnectionBuildParameters.NONE);

        Assert.assertTrue(result.isPresent());
        Connection connection = result.get().getOne();
        Assert.assertTrue(connection instanceof RelationalDatabaseConnection);
        RelationalDatabaseConnection relConn = (RelationalDatabaseConnection) connection;
        Assert.assertEquals(DatabaseType.H2, relConn.databaseType);
        Assert.assertEquals(DatabaseType.H2, relConn.type);
        Assert.assertEquals("myDB", relConn.element);
        Assert.assertTrue(relConn.datasourceSpecification instanceof LocalH2DatasourceSpecification);
        LocalH2DatasourceSpecification h2Spec = (LocalH2DatasourceSpecification) relConn.datasourceSpecification;
        Assert.assertNotNull(h2Spec.testDataSetupCsv);
        Assert.assertTrue(h2Spec.testDataSetupCsv.contains("PersonTable"));
    }

    @Test
    public void testBuildRelationalTestConnection_DuckDB()
    {
        RelationalConnectionFactory factory = new RelationalConnectionFactory();
        RelationalCSVData data = createSimpleRelationalCSVData();
        TestConnectionBuildParameters duckDBParams = TestConnectionBuildParameters.newBuilder().withIsRelation(true).build();

        Optional<Pair<Connection, List<Closeable>>> result = factory.buildRelationalTestConnection("myDB", data, duckDBParams);

        Assert.assertTrue(result.isPresent());
        Connection connection = result.get().getOne();
        Assert.assertTrue(connection instanceof RelationalDatabaseConnection);
        RelationalDatabaseConnection relConn = (RelationalDatabaseConnection) connection;
        Assert.assertEquals(DatabaseType.DuckDB, relConn.databaseType);
        Assert.assertEquals(DatabaseType.DuckDB, relConn.type);
        Assert.assertEquals("myDB", relConn.element);
        Assert.assertTrue(relConn.datasourceSpecification instanceof DuckDBDatasourceSpecification);
        DuckDBDatasourceSpecification duckDBSpec = (DuckDBDatasourceSpecification) relConn.datasourceSpecification;
        Assert.assertEquals("", duckDBSpec.path);
        Assert.assertNotNull(duckDBSpec.testDataSetupCsv);
        Assert.assertTrue("testDataSetupCsv should contain table data", duckDBSpec.testDataSetupCsv.contains("PersonTable"));
    }

    @Test
    public void testBuildRelationalTestConnection_NullElement()
    {
        RelationalConnectionFactory factory = new RelationalConnectionFactory();
        RelationalCSVData data = createSimpleRelationalCSVData();

        Optional<Pair<Connection, List<Closeable>>> h2Result = factory.buildRelationalTestConnection(null, data, TestConnectionBuildParameters.NONE);
        Assert.assertTrue(h2Result.isPresent());
        Assert.assertNull((h2Result.get().getOne()).element);

        Optional<Pair<Connection, List<Closeable>>> duckDBResult = factory.buildRelationalTestConnection(null, data, TestConnectionBuildParameters.newBuilder().withIsRelation(true).build());
        Assert.assertTrue(duckDBResult.isPresent());
        Assert.assertNull((duckDBResult.get().getOne()).element);
    }

    @Test
    public void testBuildRelationalTestConnection_MultipleTables()
    {
        RelationalConnectionFactory factory = new RelationalConnectionFactory();
        RelationalCSVData data = new RelationalCSVData();
        RelationalCSVTable table1 = new RelationalCSVTable();
        table1.schema = "default";
        table1.table = "PersonTable";
        table1.values = "id,name\n1,Alice\n";
        RelationalCSVTable table2 = new RelationalCSVTable();
        table2.schema = "default";
        table2.table = "FirmTable";
        table2.values = "id,name\n10,GS\n";
        data.tables = new java.util.ArrayList<>();
        data.tables.add(table1);
        data.tables.add(table2);

        TestConnectionBuildParameters duckDBParams = TestConnectionBuildParameters.newBuilder().withIsRelation(true).build();
        Optional<Pair<Connection, List<Closeable>>> result = factory.buildRelationalTestConnection("myDB", data, duckDBParams);
        Assert.assertTrue(result.isPresent());
        DuckDBDatasourceSpecification duckDBSpec = (DuckDBDatasourceSpecification) ((RelationalDatabaseConnection) result.get().getOne()).datasourceSpecification;
        Assert.assertEquals("", duckDBSpec.path);
        Assert.assertNotNull(duckDBSpec.testDataSetupCsv);
        Assert.assertTrue("testDataSetupCsv should contain data for both tables", duckDBSpec.testDataSetupCsv.contains("PersonTable") && duckDBSpec.testDataSetupCsv.contains("FirmTable"));
    }

    @Test
    public void testTryBuildTestConnection_H2VsDuckDB()
    {
        RelationalConnectionFactory factory = new RelationalConnectionFactory();
        RelationalCSVData data = createSimpleRelationalCSVData();

        RelationalDatabaseConnection sourceConn = new RelationalDatabaseConnection();
        sourceConn.element = "myDB";

        Optional<Pair<Connection, List<Closeable>>> h2Result = factory.tryBuildTestConnection(sourceConn, Collections.singletonList(data), TestConnectionBuildParameters.NONE);
        Assert.assertTrue(h2Result.isPresent());
        Assert.assertEquals(DatabaseType.H2, ((RelationalDatabaseConnection) h2Result.get().getOne()).databaseType);

        Optional<Pair<Connection, List<Closeable>>> duckDBResult = factory.tryBuildTestConnection(sourceConn, Collections.singletonList(data), TestConnectionBuildParameters.newBuilder().withIsRelation(true).build());
        Assert.assertTrue(duckDBResult.isPresent());
        Assert.assertEquals(DatabaseType.DuckDB, ((RelationalDatabaseConnection) duckDBResult.get().getOne()).databaseType);
    }

    @Test
    public void testTryBuildTestConnection_NonRelationalDataReturnsEmpty()
    {
        RelationalConnectionFactory factory = new RelationalConnectionFactory();
        RelationalDatabaseConnection sourceConn = new RelationalDatabaseConnection();
        sourceConn.element = "myDB";

        Optional<Pair<Connection, List<Closeable>>> result = factory.tryBuildTestConnection(sourceConn, Collections.emptyList(), TestConnectionBuildParameters.newBuilder().withIsRelation(true).build());
        Assert.assertFalse(result.isPresent());
    }
}
