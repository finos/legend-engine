// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.tempTableVisitor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.StreamResultToTempTableVisitor;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalTdsInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.h2.tools.Server;
import org.junit.Assert;
import org.junit.BeforeClass;

import javax.security.auth.Subject;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TestStreamResultToTempTableVisitor
{
    private final Subject subject;

    protected TestStreamResultToTempTableVisitor(Subject subject)
    {
        this.subject = subject;
    }

    @BeforeClass
    public static void beforeClass()
    {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    protected void testTempTableCreationUsingRealizedRelationalResult(DataSourceSpecification dataSourceSpecification)
    {
        Pair<RealizedRelationalResult, String> resultAndExpectedOutput = getRealizedRelationalResultAndExpectedOutput();
        testTempTableCreation(dataSourceSpecification, resultAndExpectedOutput.getOne(), resultAndExpectedOutput.getTwo());
    }

    protected void testTempTableCreationUsingRelationalResult(DataSourceSpecification dataSourceSpecification)
    {
        runWithLocalH2ServerCreation((server, port) ->
        {
            Pair<RelationalResult, String> resultAndExpectedOutput = getRelationalResultAndExpectedOutput(getLocalH2Specification().getConnectionUsingSubject(subject));
            testTempTableCreation(dataSourceSpecification, resultAndExpectedOutput.getOne(), resultAndExpectedOutput.getTwo());
        });
    }

    private void testTempTableCreation(DataSourceSpecification dataSourceSpecification, StreamingResult streamingResult, String expected)
    {
        RelationalExecutionConfiguration relationalExecutionConfiguration = new RelationalExecutionConfiguration();
        relationalExecutionConfiguration.tempPath = "/tmp/";
        RelationalDatabaseCommands dbCommands = dataSourceSpecification.getDatabaseManager().relationalDatabaseSupport();

        String tempTableName = "temp_table_" + System.nanoTime();
        String processedTempTableName = dbCommands.processTempTableName(tempTableName);

        try (Connection connection = dataSourceSpecification.getConnectionUsingSubject(subject))
        {
            dbCommands.accept(new StreamResultToTempTableVisitor(relationalExecutionConfiguration, connection, streamingResult, processedTempTableName, "UTC"));

            try (Statement statement = connection.createStatement())
            {
                ResultSet resultSet = statement.executeQuery(String.format("SELECT * FROM %s", processedTempTableName));
                String tableContent = serializeResultSetToCSV(resultSet);
                statement.execute(dbCommands.dropTempTable(processedTempTableName));

                Assert.assertEquals(expected, tableContent);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String serializeResultSetToCSV(ResultSet resultSet) throws IOException, SQLException
    {
        StringBuilder resultBuilder = new StringBuilder();
        try (CSVPrinter csvPrinter = new CSVPrinter(resultBuilder, CSVFormat.DEFAULT))
        {
            csvPrinter.printRecords(resultSet);
            csvPrinter.flush();
            resultSet.close();
            return resultBuilder.toString();
        }
    }

    private static Pair<RealizedRelationalResult, String> getRealizedRelationalResultAndExpectedOutput()
    {
        RealizedRelationalResult result = RealizedRelationalResult.emptyRealizedRelationalResult(getColumns());

        List<Object> row1 = Arrays.asList(1, 1.1, "String 1", PureDate.parsePureDate("2021-03-01T14:00:00"), PureDate.parsePureDate("2021-03-01"));
        result.addRow(row1, row1);

        List<Object> row2 = Arrays.asList(2, 2.2, "String 2", PureDate.parsePureDate("2021-03-02T14:00:00"), PureDate.parsePureDate("2021-03-02"));
        result.addRow(row2, row2);

        List<Object> row3 = Arrays.asList(3, 3.3, "String 3", PureDate.parsePureDate("7900-01-01T14:00:00"), PureDate.parsePureDate("7900-12-31"));
        result.addRow(row3, row3);

        List<Object> row4 = Arrays.asList(4, 4.4, "String 4", PureDate.parsePureDate("1800-03-02T14:00:00.000123"), PureDate.parsePureDate("1800-03-02"));
        result.addRow(row4, row4);

        List<Object> row5 = Arrays.asList(5, 5.5, "String 5", PureDate.parsePureDate("2021-03-02T14:00:00.000123"), PureDate.parsePureDate("2021-03-02"));
        result.addRow(row5, row5);

        String expectedOutput =
                "1,1.1,String 1,2021-03-01 14:00:00.0,2021-03-01\r\n" +
                        "2,2.2,String 2,2021-03-02 14:00:00.0,2021-03-02\r\n" +
                        "3,3.3,String 3,7900-01-01 14:00:00.0,7900-12-31\r\n" +
                        "4,4.4,String 4,1800-03-02 14:00:00.000123,1800-03-02\r\n" +
                        "5,5.5,String 5,2021-03-02 14:00:00.000123,2021-03-02\r\n";

        return Tuples.pair(result, expectedOutput);
    }

    private static Pair<RelationalResult, String> getRelationalResultAndExpectedOutput(Connection sourceConnection)
    {
        List<SQLResultColumn> columns = getColumns();
        String sourceTable = "temp_table_source_" + System.nanoTime();

        try (Statement statement = sourceConnection.createStatement())
        {
            String createStmt = "CREATE TABLE %s(%s)";
            String insertStmt = "INSERT INTO %s(%s) VALUES (%s), (%s), (%s), (%s), (%s)";
            String row1 = "1,1.1,'String 1','2021-02-01T15:00:00','2021-02-01'";
            String row2 = "2,2.2,'String 2','2021-02-02T15:00:00','2021-02-02'";
            String row3 = "3,3.3,'String 3','7900-01-01 14:00:00.0','7900-12-31'";
            String row4 = "4,4.4,'String 4','1800-03-02 14:00:00.000123','1800-03-02'";
            String row5 = "5,5.5,'String 5','2021-03-02 14:00:00.000123','2021-03-02'";

            statement.execute(String.format(createStmt, sourceTable, columns.stream().map(x -> x.label + " " + x.dataType).collect(Collectors.joining(", "))));
            statement.execute(String.format(insertStmt, sourceTable, columns.stream().map(x -> x.label).collect(Collectors.joining(", ")), row1, row2, row3, row4, row5));
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        SQLExecutionNode sqlExecutionNode = new SQLExecutionNode();
        RelationalDatabaseConnection databaseConnection = new RelationalDatabaseConnection();
        databaseConnection.type = DatabaseType.H2;
        databaseConnection.databaseType = DatabaseType.H2;
        databaseConnection.datasourceSpecification = new LocalH2DatasourceSpecification();
        databaseConnection.authenticationStrategy = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy();
        sqlExecutionNode.connection = databaseConnection;
        sqlExecutionNode.resultColumns = columns;
        SQLExecutionResult sqlExecutionResult = new SQLExecutionResult(
                Collections.singletonList(new RelationalExecutionActivity("SELECT * from " + sourceTable)),
                sqlExecutionNode, "H2", "UTC", sourceConnection, null, Collections.singletonList(sourceTable), null
        );

        RelationalTdsInstantiationExecutionNode node = new RelationalTdsInstantiationExecutionNode();
        TDSResultType tdsResultType = new TDSResultType();
        tdsResultType.tdsColumns = columns.stream().map(c ->
        {
            TDSColumn tc =  new TDSColumn();
            tc.name = c.label.toUpperCase();
            tc.relationalType = c.dataType;
            tc.type = c.labelTypePair().getTwo();
            return tc;
        }).collect(Collectors.toList());
        node.resultType = tdsResultType;
        RelationalResult result = new RelationalResult(sqlExecutionResult, node);

        String expectedOutput =
                "1,1.1,String 1,2021-02-01 15:00:00.0,2021-02-01\r\n" +
                        "2,2.2,String 2,2021-02-02 15:00:00.0,2021-02-02\r\n" +
                        "3,3.3,String 3,7900-01-01 14:00:00.0,7900-12-31\r\n" +
                        "4,4.4,String 4,1800-03-02 14:00:00.000123,1800-03-02\r\n" +
                        "5,5.5,String 5,2021-03-02 14:00:00.000123,2021-03-02\r\n";

        return Tuples.pair(result, expectedOutput);
    }

    private static List<SQLResultColumn> getColumns()
    {
        return Arrays.asList(
                new SQLResultColumn("integer_Column", "INT"),
                new SQLResultColumn("float_Column", "FLOAT"),
                new SQLResultColumn("string_Column", "VARCHAR(128)"),
                new SQLResultColumn("datetime_Column", "TIMESTAMP"),
                new SQLResultColumn("date_Column", "DATE")
        );
    }

    private static LocalH2DataSourceSpecification getLocalH2Specification()
    {
        return new LocalH2DataSourceSpecification(
                Lists.mutable.empty(),
                new H2Manager(),
                new TestDatabaseAuthenticationStrategy()
        );
    }

    private static void runWithLocalH2ServerCreation(BiConsumer<Server, Integer> toRun)
    {
        int port = DynamicPortGenerator.generatePort();
        Server server = null;
        try
        {
            server = AlloyH2Server.startServer(port);
            toRun.accept(server, port);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (server != null)
            {
                server.stop();
            }
        }
    }
}