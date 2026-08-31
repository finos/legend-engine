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

package org.finos.legend.engine.plan.execution.stores.relational;

import org.apache.commons.csv.CSVFormat;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RealizedRelationalResultCSVSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class TestStreamResultToTempTableVisitor
{
    @Test
    public void buildSerializerAndColumns_realizedRelationalResult()
    {
        RealizedRelationalResult rrr = mock(RealizedRelationalResult.class);
        rrr.columns = Arrays.asList(new SQLResultColumn("id", "INT"), new SQLResultColumn("name", "VARCHAR"));

        StreamResultToTempTableVisitor v = newVisitor(rrr);
        StreamResultToTempTableVisitor.SerializerAndColumns sc = v.buildSerializerAndColumns(new StubCommands());

        assertTrue(sc.serializer instanceof RealizedRelationalResultCSVSerializer);
        assertEquals(2, sc.columns.size());
        assertEquals("id", sc.columns.get(0).name);
        assertEquals("INT", sc.columns.get(0).type);
        assertEquals("name", sc.columns.get(1).name);
        assertEquals("VARCHAR", sc.columns.get(1).type);
    }

    @Test
    public void buildSerializerAndColumns_unsupportedResult_throws()
    {
        StreamingResult unsupported = mock(StreamingResult.class);
        StreamResultToTempTableVisitor v = newVisitor(unsupported);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> v.buildSerializerAndColumns(new StubCommands()));
        assertTrue(ex.getMessage().startsWith("Result not supported yet:"));
    }

    @Test
    public void streamResultToTableViaStream_pipesCsvToIngest() throws Exception
    {
        RealizedRelationalResult rrr = mock(RealizedRelationalResult.class);
        rrr.columns = Arrays.asList(new SQLResultColumn("id", "INT"), new SQLResultColumn("name", "VARCHAR"));
        rrr.resultSetRows = Arrays.asList(
                Arrays.<Object>asList(1, "alice"),
                Arrays.<Object>asList(2, "bob"));

        CapturingCommands cmds = new CapturingCommands();
        StreamResultToTempTableVisitor v = newVisitor(rrr);

        v.streamResultToTableViaStream(cmds);

        assertEquals("myTable", cmds.capturedTable);
        assertEquals(Arrays.asList("id", "name"), cmds.capturedColumnNames());
        String csv = cmds.capturedCsv.toString("UTF-8").replace("\r\n", "\n").trim();
        assertEquals("1,alice\n2,bob", csv);
    }

    @Test
    public void streamResultToTableViaStream_propagatesConsumerFailure()
    {
        RealizedRelationalResult rrr = mock(RealizedRelationalResult.class);
        rrr.columns = Collections.singletonList(new SQLResultColumn("id", "INT"));
        rrr.resultSetRows = Collections.singletonList(Arrays.<Object>asList(1));

        RelationalDatabaseCommands failing = new StubCommands()
        {
            @Override
            public void ingestFromStream(Connection connection, String tableName, List<Column> columns, InputStream csvInputStream)
            {
                throw new RuntimeException("boom");
            }
        };

        StreamResultToTempTableVisitor v = newVisitor(rrr);
        Exception ex = assertThrows(Exception.class, () -> v.streamResultToTableViaStream(failing));
        // consumer error surfaces as-is (RuntimeException from ingestFromStream)
        assertEquals("boom", ex.getMessage());
    }

    private static StreamResultToTempTableVisitor newVisitor(StreamingResult result)
    {
        StreamResultToTempTableVisitor v = new StreamResultToTempTableVisitor(null, null, result, "myTable", "GMT");
        v.ingestionMethod = IngestionMethod.CLIENT_STREAM;
        return v;
    }

    private static class StubCommands extends RelationalDatabaseCommands
    {
        @Override
        public String dropTempTable(String tableName)
        {
            return "";
        }

        @Override
        public List<String> createAndLoadTempTable(String tableName, List<Column> columns, String optionalCSVFileLocation)
        {
            return Collections.emptyList();
        }

        @Override
        public <T> T accept(RelationalDatabaseCommandsVisitor<T> visitor)
        {
            return visitor.visit(this);
        }

        @Override
        public IngestionMethod getDefaultIngestionMethod()
        {
            return IngestionMethod.CLIENT_STREAM;
        }

        @Override
        public boolean supportsHeaderOnCsvFile()
        {
            return false;
        }

        @Override
        public CSVFormat getCsvFormatForTempFile()
        {
            return CSVFormat.DEFAULT;
        }
    }

    /** Drains the streamed CSV into a byte buffer for assertions. */
    private static class CapturingCommands extends StubCommands
    {
        String capturedTable;
        List<Column> capturedColumns;
        final ByteArrayOutputStream capturedCsv = new ByteArrayOutputStream();

        List<String> capturedColumnNames()
        {
            return capturedColumns == null ? null : capturedColumns.stream().map(c -> c.name).collect(java.util.stream.Collectors.toList());
        }

        @Override
        public void ingestFromStream(Connection connection, String tableName, List<Column> columns, InputStream csvInputStream) throws IOException
        {
            this.capturedTable = tableName;
            this.capturedColumns = columns;
            byte[] buf = new byte[4096];
            int n;
            while ((n = csvInputStream.read(buf)) != -1)
            {
                capturedCsv.write(buf, 0, n);
            }
        }
    }
}
