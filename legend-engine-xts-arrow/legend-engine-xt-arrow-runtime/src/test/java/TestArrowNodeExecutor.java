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

import java.io.IOException;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.external.format.arrow.ArrowRuntimeExtension;
import org.finos.legend.engine.external.shared.runtime.write.ExternalFormatSerializeResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeTDSExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.*;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.mockito.ArgumentMatchers.any;

public class TestArrowNodeExecutor

{

    @Test
    public void testExternalize() throws Exception
    {
        ArrowRuntimeExtension extension = new ArrowRuntimeExtension();
        ExternalFormatExternalizeTDSExecutionNode node = new ExternalFormatExternalizeTDSExecutionNode();
        //create a real result from H2
        RelationalExecutionNode mockExecutionNode = Mockito.mock(RelationalExecutionNode.class);
        DatabaseConnection mockDatabaseConnection = Mockito.mock(DatabaseConnection.class);

        mockExecutionNode.connection = mockDatabaseConnection;
        Mockito.when(mockDatabaseConnection.accept(any())).thenReturn(false);
        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/test;TIME ZONE=America/New_York", "", "");
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            //setup table
            conn.createStatement().execute("DROP TABLE IF EXISTS testtable");
            conn.createStatement().execute("DROP TABLE IF EXISTS testtableJoin");

            conn.createStatement().execute("Create Table testtable (testInt INTEGER, testString VARCHAR(255), testDate TIMESTAMP, testBool BOOLEAN)");
            conn.createStatement().execute("Create Table testtableJoin (testIntR INTEGER, testStringR VARCHAR(255)  PRIMARY KEY )");

            conn.createStatement().execute("INSERT INTO  testtable (testInt, testString, testDate, testBool) VALUES(1,'A', '2020-01-01 00:00:00-05:00',true),( 2,null, '2020-01-01 00:00:00-02:00',false ),( 3,'B', '2020-01-01 00:00:00-05:00',false )");
            conn.createStatement().execute("INSERT INTO  testtableJoin (testIntR, testStringR) VALUES(6,'A'), (1,'B')");

            RelationalResult result = new RelationalResult(FastList.newListWith(new RelationalExecutionActivity("SELECT * FROM testtable left join  testtableJoin on testtable.testInt=testtableJoin.testIntR", null)), mockExecutionNode, FastList.newListWith(new SQLResultColumn("testInt", "INTEGER"), new SQLResultColumn("testStringR", "VARCHAR"), new SQLResultColumn("testString", "VARCHAR"), new SQLResultColumn("testDate", "TIMESTAMP"), new SQLResultColumn("testBool", "TIMESTAMP")), null, "America/New_York", conn, Identity.getAnonymousIdentity(), null, null, new RequestContext());

            ExternalFormatSerializeResult nodeExecute = (ExternalFormatSerializeResult) extension.executeExternalizeTDSExecutionNode(node, result, Identity.getAnonymousIdentity(), null);


            nodeExecute.stream(outputStream, SerializationFormat.DEFAULT);

            String expected = "TESTINT\tTESTSTRING\tTESTDATE\tTESTBOOL\tTESTINTR\tTESTSTRINGR\n" +
                    "1\tA\t1577854800000\ttrue\t1\tB\n" +
                    "2\tnull\t1577844000000\tfalse\tnull\tnull\n" +
                    "3\tB\t1577854800000\tfalse\tnull\tnull\n";
            assertArrow(outputStream, expected);
        }

    }

    @Test
    public void testExternalizeAsString() throws Exception
    {
        ArrowRuntimeExtension extension = new ArrowRuntimeExtension();
        ExternalFormatExternalizeTDSExecutionNode node = new ExternalFormatExternalizeTDSExecutionNode();
        //create a real result from H2
        RelationalExecutionNode mockExecutionNode = Mockito.mock(RelationalExecutionNode.class);
        DatabaseConnection mockDatabaseConnection = Mockito.mock(DatabaseConnection.class);

        mockExecutionNode.connection = mockDatabaseConnection;
        Mockito.when(mockDatabaseConnection.accept(any())).thenReturn(false);
        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/test;TIME ZONE=America/New_York", "", "");
        )

        {
            //setup table
            conn.createStatement().execute("DROP TABLE IF EXISTS testtable");
            conn.createStatement().execute("Create Table testtable (testInt INTEGER, testString VARCHAR(255), testDate TIMESTAMP, testBool BOOLEAN)");
            conn.createStatement().execute("INSERT INTO  testtable (testInt, testString, testDate, testBool) VALUES(1,'A', '2020-01-01 00:00:00-05:00',true),( 2,'B', '2020-01-01 00:00:00-02:00',false ),( 3,'B', '2020-01-01 00:00:00-05:00',false )");

            RelationalResult result = new RelationalResult(FastList.newListWith(new RelationalExecutionActivity("SELECT * FROM testtable", null)), mockExecutionNode, FastList.newListWith(new SQLResultColumn("testInt", "INTEGER"), new SQLResultColumn("testString", "VARCHAR"), new SQLResultColumn("testDate", "TIMESTAMP"), new SQLResultColumn("testBool", "TIMESTAMP")), null, "America/New_York", conn, Identity.getAnonymousIdentity(), null, null, new RequestContext());

            ExternalFormatSerializeResult nodeExecute = (ExternalFormatSerializeResult) extension.executeExternalizeTDSExecutionNode(node, result, Identity.getAnonymousIdentity(), null);

            String expected = "TESTINT\tTESTSTRING\tTESTDATE\tTESTBOOL\n" +
                    "1\tA\t1577854800000\ttrue\n" +
                    "2\tB\t1577844000000\tfalse\n" +
                    "3\tB\t1577854800000\tfalse\n";

            String outputasString = nodeExecute.flush(nodeExecute.getSerializer(SerializationFormat.DEFAULT));

            Assert.assertEquals(expected, outputasString);
        }


    }

    private void assertArrow(ByteArrayOutputStream actualOutputStream, String expectedTSV) throws IOException //input a TSV String
    {
        actualOutputStream.flush();
        ByteArrayInputStream input = new ByteArrayInputStream(actualOutputStream.toByteArray());
        actualOutputStream.close();

        VectorSchemaRoot actualSchema = null;
        String actualTSV = "";
        try (
                BufferAllocator rootAllocator = new RootAllocator();
                ArrowStreamReader actualReader = new ArrowStreamReader(input, rootAllocator)

        )
        {
            while (actualReader.loadNextBatch())
            {
                actualSchema = actualReader.getVectorSchemaRoot();
                actualTSV += actualSchema.contentToTSVString();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        Assert.assertEquals(expectedTSV, actualTSV);


    }
}
