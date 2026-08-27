//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds;

import net.snowflake.client.api.connection.SnowflakeConnection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeCommands;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestSnowflakeCommands
{
    private SnowflakeCommands snowflakeCommands = new SnowflakeCommands();

    @Test
    public void testDefaultIngestionMethodIsStream()
    {
        assertEquals(IngestionMethod.CLIENT_STREAM, snowflakeCommands.getDefaultIngestionMethod());
    }

    @Test
    public void testCsvFormatForTempFileUsesAllNonNullQuoteMode()
    {
        CSVFormat format = snowflakeCommands.getCsvFormatForTempFile();

        assertNotNull(format);
        assertEquals(QuoteMode.ALL_NON_NULL, format.getQuoteMode());
        assertEquals(Character.valueOf('"'), format.getQuoteCharacter());
    }

    @Test
    public void testCsvFormatForTempFileQuotesNonNullValuesAndPreservesEmbeddedNewlines() throws IOException
    {
        CSVFormat format = snowflakeCommands.getCsvFormatForTempFile();

        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, format))
        {
            printer.printRecord(Arrays.asList("hello", "", null, 42, "line1\nline2,\"still one field\""));
            printer.printRecord(Arrays.asList("x", "y", "z", 0, "plain"));
        }

        String expected =
                "\"hello\",\"\",,\"42\",\"line1\nline2,\"\"still one field\"\"\"\r\n"
                        + "\"x\",\"y\",\"z\",\"0\",\"plain\"\r\n";
        assertEquals(expected, out.toString());
    }

    @Test
    public void testIngestFromStreamRunsFullDdlSequenceAndUploadsStream() throws Exception
    {
        String table = "LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.TMP_T";
        List<Column> columns = Arrays.asList(new Column("id", "INT"), new Column("flag", "BIT"));
        InputStream csv = new ByteArrayInputStream("1,true\n".getBytes());

        SnowflakeConnection snowflakeConnection = mock(SnowflakeConnection.class);
        Statement ddlStmt = mock(Statement.class);
        Statement copyStmt = mock(Statement.class);
        Statement dropStmt = mock(Statement.class);
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(ddlStmt, copyStmt, dropStmt);
        when(connection.unwrap(SnowflakeConnection.class)).thenReturn(snowflakeConnection);

        snowflakeCommands.ingestFromStream(connection, table, columns, csv);

        // DDL block: three statements executed in order on the same Statement
        ArgumentCaptor<String> ddlSql = ArgumentCaptor.forClass(String.class);
        verify(ddlStmt, times(3)).execute(ddlSql.capture());
        List<String> ddlCalls = ddlSql.getAllValues();
        assertEquals("Drop table if exists " + table, ddlCalls.get(0));
        assertTrue(ddlCalls.get(1).startsWith("CREATE TEMPORARY TABLE " + table));
        // BIT is remapped to BOOLEAN
        assertTrue(ddlCalls.get(1).contains("id INT"));
        assertTrue(ddlCalls.get(1).contains("flag BOOLEAN"));
        assertEquals("CREATE OR REPLACE TEMPORARY STAGE " + snowflakeCommands.tempStageName(), ddlCalls.get(2));
        verify(ddlStmt).close();

        // uploadStream: same InputStream instance, staged onto configured stage, csv filename
        ArgumentCaptor<String> stageName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<InputStream> streamArg = ArgumentCaptor.forClass(InputStream.class);
        verify(snowflakeConnection).uploadStream(stageName.capture(), fileName.capture(), streamArg.capture());
        assertEquals(snowflakeCommands.tempStageName(), stageName.getValue());
        assertTrue(fileName.getValue().startsWith("legend_stream_"));
        assertTrue(fileName.getValue().endsWith(".csv"));
        assertSame(csv, streamArg.getValue());

        // COPY INTO references the staged .gz object and target table
        ArgumentCaptor<String> copySql = ArgumentCaptor.forClass(String.class);
        verify(copyStmt).execute(copySql.capture());
        String copy = copySql.getValue();
        assertTrue(copy.startsWith("COPY INTO " + table + " FROM @" + snowflakeCommands.tempStageName() + "/"));
        assertTrue(copy.contains(fileName.getValue() + ".gz"));
        assertTrue(copy.contains("ON_ERROR = ABORT_STATEMENT"));
        verify(copyStmt).close();

        // DROP STAGE in finally
        verify(dropStmt).execute("DROP STAGE " + snowflakeCommands.tempStageName());
        verify(dropStmt).close();

        verify(connection, times(3)).createStatement();
    }

    @Test
    public void testIngestFromStreamPropagatesCopyFailureAndSwallowsDropStageError() throws Exception
    {
        String table = "T";
        List<Column> columns = Collections.singletonList(new Column("id", "INT"));
        InputStream csv = new ByteArrayInputStream(new byte[0]);

        SnowflakeConnection snowflakeConnection = mock(SnowflakeConnection.class);
        Statement ddlStmt = mock(Statement.class);
        Statement copyStmt = mock(Statement.class);
        Statement dropStmt = mock(Statement.class);
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(ddlStmt, copyStmt, dropStmt);
        when(connection.unwrap(SnowflakeConnection.class)).thenReturn(snowflakeConnection);

        SQLException copyFailure = new SQLException("copy exploded");
        doThrow(copyFailure).when(copyStmt).execute(anyString());
        // Drop stage also fails — must be swallowed so the copy error is what surfaces
        doThrow(new SQLException("drop stage exploded")).when(dropStmt).execute(anyString());

        try
        {
            snowflakeCommands.ingestFromStream(connection, table, columns, csv);
            fail("expected copy failure to propagate");
        }
        catch (SQLException e)
        {
            assertSame(copyFailure, e);
        }

        // uploadStream still ran before the failure
        verify(snowflakeConnection).uploadStream(anyString(), anyString(), any(InputStream.class));
        // drop stage was attempted despite copy failure
        verify(dropStmt).execute("DROP STAGE " + snowflakeCommands.tempStageName());
    }
}
