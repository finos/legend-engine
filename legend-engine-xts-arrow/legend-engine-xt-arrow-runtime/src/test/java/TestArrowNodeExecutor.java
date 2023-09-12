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
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.JavaPlatformImplementation;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.externalFormat.ExternalFormatExternalizeExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.api.request.RequestContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;

import static org.mockito.ArgumentMatchers.any;

public class TestArrowNodeExecutor

{
    @Test
    public void testExternalize() throws Exception
    {
        ArrowRuntimeExtension extension = new ArrowRuntimeExtension();
        ExternalFormatExternalizeExecutionNode node = new ExternalFormatExternalizeExecutionNode();
        node.implementation = new JavaPlatformImplementation();
        //create a real result from H2
        RelationalExecutionNode mockExecutionNode = Mockito.mock(RelationalExecutionNode.class);
        DatabaseConnection mockDatabaseConnection = Mockito.mock(DatabaseConnection.class);

        mockExecutionNode.connection = mockDatabaseConnection;
        Mockito.when(mockDatabaseConnection.accept(any())).thenReturn(false);
        try (Connection conn = DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); )
        {
            //setup table
            conn.createStatement().execute("DROP TABLE IF EXISTS testtable");
            conn.createStatement().execute("Create Table testtable (testC INTEGER )");
            conn.createStatement().execute("INSERT INTO  testtable (testC) VALUES(1)");

            RelationalResult result = new RelationalResult(FastList.newListWith(new RelationalExecutionActivity("SELECT testC FROM testtable", null)), mockExecutionNode, FastList.newListWith(new SQLResultColumn("testC", "INTEGER")), null, null, conn, null, null, null, new RequestContext());

            ExternalFormatSerializeResult nodeExecute = (ExternalFormatSerializeResult) extension.executeExternalizeExecutionNode(node, result, null, null);

            nodeExecute.stream(outputStream, SerializationFormat.DEFAULT);
            outputStream.flush();
            ByteArrayInputStream input = new ByteArrayInputStream(outputStream.toByteArray());
            outputStream.close();
            assertArrow(input, "TESTC\n" +
                    "1\n");
        }

    }


    private String generateExpectedArrow(InputStream input)
    {
        String response = "";
        try (
                BufferAllocator rootAllocator = new RootAllocator();
                ArrowStreamReader reader = new ArrowStreamReader(input, rootAllocator)
        )
        {
            while (reader.loadNextBatch())
            {
                VectorSchemaRoot vectorSchemaRootRecover = reader.getVectorSchemaRoot();
                System.out.print(vectorSchemaRootRecover.contentToTSVString());
                response += vectorSchemaRootRecover.contentToTSVString();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();

        }
        return response;
    }


    private void assertArrow(InputStream actualInput, String expectedTSV) //input a TSV String
    {

        VectorSchemaRoot expectedSchema = null;
        VectorSchemaRoot actualSchema = null;
        String actualTSV = "";
        try (
                BufferAllocator rootAllocator = new RootAllocator();
                ArrowStreamReader actualReader = new ArrowStreamReader(actualInput, rootAllocator)

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
