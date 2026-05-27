// Copyright 2021 Goldman Sachs
// Licensed under the Apache License, Version 2.0
package org.finos.legend.engine.plan.execution.stores.relational.serialization;

import com.github.luben.zstd.ZstdInputStream;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.ipc.ArrowStreamReader;
import org.apache.arrow.vector.types.pojo.Schema;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class TestRelationalResultToArrowIPCSerializer
{
    private Connection connection;
    private Statement statement;
    private ResultSet resultSet;
    private RelationalResult relationalResult;

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Build the 4 SQLResultColumn entries that mirror the H2 table.
     */
    private static List<SQLResultColumn> buildSqlColumns()
    {
        SQLResultColumn id        = new SQLResultColumn("\"id\"",         "INTEGER");
        SQLResultColumn firstName = new SQLResultColumn("\"first_name\"", "VARCHAR");
        SQLResultColumn age       = new SQLResultColumn("\"age\"",        "INTEGER");
        SQLResultColumn salary    = new SQLResultColumn("\"salary\"",     "DOUBLE");
        return Arrays.asList(id, firstName, age, salary);
    }

    /**
     * Set a public (non-final) field on a mocked RelationalResult via reflection.
     * Mockito cannot stub fields, only methods.
     */
    private static void setField(Object target, String fieldName, Object value) throws Exception
    {
        Field f = RelationalResult.class.getField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Wire a mocked RelationalResult so it behaves like a real one against the
     * supplied JDBC ResultSet and the H2 schema we created in setUp().
     */
    private static RelationalResult wireMock(ResultSet rs) throws Exception
    {
        RelationalResult rr = mock(RelationalResult.class);

        // Methods used by the serializer
        when(rr.getResultSet()).thenReturn(rs);
        when(rr.getSQLResultColumns()).thenReturn(buildSqlColumns());
        when(rr.getColumnListForSerializer()).thenReturn(java.util.Collections.emptyList());

        // getValue(i) is invoked per column inside writeRow(...).
        // Delegate to the underlying ResultSet.getObject(i).
        when(rr.getValue(anyInt())).thenAnswer(inv -> rs.getObject((Integer) inv.getArgument(0)));

        // Public fields read directly by the serializer
        setField(rr, "columnCount", 4);
        // builder / activities / generationInfo / topSpan stay null - safe.

        return rr;
    }

    // ------------------------------------------------------------------
    // Setup / Teardown
    // ------------------------------------------------------------------

    @Before
    public void setUp() throws Exception
    {
        // 1. Spin up an in-memory H2 database with a tiny dataset.
        connection = DriverManager.getConnection(
                "jdbc:h2:mem:arrowTest;DB_CLOSE_DELAY=-1", "sa", "");
        statement = connection.createStatement();
        statement.execute(
                "CREATE TABLE person (" +
                        "  id INT PRIMARY KEY, " +
                        "  first_name VARCHAR(50), " +
                        "  age INT, " +
                        "  salary DOUBLE)");
        statement.execute("INSERT INTO person VALUES (1, 'Alice', 30, 75000.50)");
        statement.execute("INSERT INTO person VALUES (2, 'Bob',   45, 92500.00)");
        statement.execute("INSERT INTO person VALUES (3, 'Carol', 28, 68000.75)");

        resultSet = statement.executeQuery("SELECT id, first_name, age, salary FROM person");

        // 2. Mock the RelationalResult so we don't need the full Legend stack.
        relationalResult = wireMock(resultSet);
    }

    @After
    public void tearDown() throws Exception
    {
        if (resultSet != null && !resultSet.isClosed())
        {
            resultSet.close();
        }
        if (statement != null && !statement.isClosed())
        {
            statement.execute("DROP TABLE IF EXISTS person");
            statement.close();
        }
        if (connection != null && !connection.isClosed())
        {
            connection.close();
        }
    }

    // ------------------------------------------------------------------
    // Happy-path: stream produces a valid Zstd-compressed Arrow IPC stream
    // that round-trips back to 3 rows with the expected schema.
    // ------------------------------------------------------------------
    @Test
    public void testStreamProducesValidArrowIPC() throws Exception
    {
        RelationalResultToArrowIPCSerializer serializer =
                new RelationalResultToArrowIPCSerializer(relationalResult);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.stream(out);

        byte[] bytes = out.toByteArray();
        assertTrue("Serializer should produce output", bytes.length > 0);

        // Decompress + decode the Arrow stream to verify content.
        try (BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
             ZstdInputStream zin = new ZstdInputStream(new ByteArrayInputStream(bytes));
             ArrowStreamReader reader = new ArrowStreamReader(zin, allocator))
        {
            VectorSchemaRoot root = reader.getVectorSchemaRoot();
            Schema schema = root.getSchema();
            Map<String, String> metadata = schema.getCustomMetadata();

            assertNotNull(metadata);
            assertTrue("legend.builder key must be present",
                    metadata.containsKey("legend.builder"));
            assertTrue("legend.activities key must be present",
                    metadata.containsKey("legend.activities"));
            assertTrue("legend.columns key must be present",
                    metadata.containsKey("legend.columns"));
            // generationInfo is null in this test => key should NOT be present
            assertFalse(metadata.containsKey("legend.generationInfo"));

            assertEquals(4, schema.getFields().size());
            assertEquals("ID",         schema.getFields().get(0).getName().toUpperCase());
            assertEquals("FIRST_NAME", schema.getFields().get(1).getName().toUpperCase());
            assertEquals("AGE",        schema.getFields().get(2).getName().toUpperCase());
            assertEquals("SALARY",     schema.getFields().get(3).getName().toUpperCase());

            int totalRows = 0;
            while (reader.loadNextBatch())
            {
                totalRows += root.getRowCount();
            }
            assertEquals(3, totalRows);
        }

        verify(relationalResult, times(1)).close();
    }

    // ------------------------------------------------------------------
    // Honor a custom batch size: with 3 rows and batchSize=1 we expect
    // multiple Arrow batches in the stream.
    // ------------------------------------------------------------------
    @Test
    public void testCustomBatchSizeProducesMultipleBatches() throws Exception
    {
        RelationalResultToArrowIPCSerializer serializer =
                new RelationalResultToArrowIPCSerializer(
                        relationalResult, /*batchSize*/ 1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.stream(out);

        try (BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
             ZstdInputStream zin = new ZstdInputStream(new ByteArrayInputStream(out.toByteArray()));
             ArrowStreamReader reader = new ArrowStreamReader(zin, allocator))
        {
            int batchCount = 0;
            int totalRows = 0;
            while (reader.loadNextBatch())
            {
                batchCount++;
                totalRows += reader.getVectorSchemaRoot().getRowCount();
            }
            assertEquals(3, totalRows);
            assertTrue("Expected >1 batches when batchSize=1 over 3 rows",
                    batchCount > 1);
        }
    }

    // ------------------------------------------------------------------
    // Empty ResultSet should produce a valid stream with schema and 0 rows.
    // ------------------------------------------------------------------
    @Test
    public void testEmptyResultSet() throws Exception
    {
        ResultSet emptyRs = statement.executeQuery(
                "SELECT id, first_name, age, salary FROM person WHERE id < 0");
        RelationalResult emptyResult = wireMock(emptyRs);

        RelationalResultToArrowIPCSerializer serializer =
                new RelationalResultToArrowIPCSerializer(emptyResult);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.stream(out);
        assertTrue(out.toByteArray().length > 0);

        try (BufferAllocator allocator = new RootAllocator(Long.MAX_VALUE);
             ZstdInputStream zin = new ZstdInputStream(new ByteArrayInputStream(out.toByteArray()));
             ArrowStreamReader reader = new ArrowStreamReader(zin, allocator))
        {
            int total = 0;
            while (reader.loadNextBatch())
            {
                total += reader.getVectorSchemaRoot().getRowCount();
            }
            assertEquals(0, total);
        }
        verify(emptyResult, times(1)).close();
    }

    // ------------------------------------------------------------------
    // Exceptions from the underlying ResultSet must be wrapped in
    // RuntimeException, and close() must still be invoked.
    // ------------------------------------------------------------------
    @Test
    public void testExceptionFromResultSetIsWrapped() throws Exception
    {
        RelationalResult failing = mock(RelationalResult.class);
        when(failing.getResultSet())
                .thenThrow(new RuntimeException("boom"));

        RelationalResultToArrowIPCSerializer serializer =
                new RelationalResultToArrowIPCSerializer(failing);

        try
        {
            serializer.stream(new ByteArrayOutputStream());
            fail("Expected RuntimeException");
        }
        catch (RuntimeException ex)
        {
            assertTrue(ex.getMessage().contains("boom")
                    || (ex.getCause() != null && ex.getCause().getMessage().contains("boom")));
        }

        verify(failing, times(1)).close();
    }
}