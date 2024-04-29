// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.test;

import net.steppschuh.markdowngenerator.Markdown;
import net.steppschuh.markdowngenerator.table.Table;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;

import java.util.List;
import java.util.Optional;

public class LegendDatabaseSQLTestingReport
{
    private ImmutableList<LegendDatabaseTestingReportGenerator.SQLTestSummary> summaries;

    public LegendDatabaseSQLTestingReport(List<LegendDatabaseTestingReportGenerator.SQLTestSummary> sqlTestSummaries)
    {
        this.summaries = Lists.immutable.withAll(sqlTestSummaries);
    }

    public String renderAsMarkdown()
    {
        ImmutableList<String> allDatabases = this.allDatabasesInSpecificOrder();

        Table cheatSheetTable = this.buildTestLegendTable();
        MutableMap<String, MutableMap<TestStatus, Integer>> statusesByDatabase = Maps.mutable.empty();
        Table testDetailsTable = this.buildTestDetailsTable(statusesByDatabase);
        Table statisticsTable = this.buildStatisticsTable(allDatabases, statusesByDatabase);

        StringBuilder stringBuilder = new StringBuilder();
        this.renderHeader(stringBuilder);
        stringBuilder.append("\n");
        this.renderCheatSheet(stringBuilder, cheatSheetTable);
        stringBuilder.append("\n");
        this.renderTestStatisticsTable(stringBuilder, statisticsTable);
        stringBuilder.append("\n");
        this.renderTestDetails(stringBuilder, testDetailsTable);
        return stringBuilder.toString();
    }

    public void renderHeader(StringBuilder stringBuilder)
    {
        stringBuilder.append(Markdown.heading("Legend SQL Compatibility Report", 1)).append("\n");
        stringBuilder.append("\n");
        stringBuilder.append(Markdown.heading("Overview", 2)).append("\n");
        stringBuilder.append(Markdown.text("This is a summary report of SQL tests for databases supported by Legend.")).append("\n\n");
        stringBuilder.append(Markdown.text("The report summarizes the output of each database's Junit integration test results.")).append("\n\n");
        stringBuilder.append(Markdown.text("The 'Test Name' column refers to a Pure test function.")).append("\n\n");
        stringBuilder.append("\n");
    }

    public void renderCheatSheet(StringBuilder stringBuilder, Table cheatSheetTable)
    {
        stringBuilder.append(Markdown.heading("Cheat sheet", 2)).append("\n");
        stringBuilder.append(cheatSheetTable.toString()).append("\n");
    }

    public void renderTestStatisticsTable(StringBuilder stringBuilder, Table statisticsTable)
    {
        stringBuilder.append(Markdown.heading("Test Statistics By Database", 2)).append("\n");
        stringBuilder.append(statisticsTable.toString()).append("\n");
    }

    public void renderTestDetails(StringBuilder stringBuilder, Table testDetailsTable)
    {
        stringBuilder.append(Markdown.heading("Test Details By Database", 2)).append("\n");
        stringBuilder.append(testDetailsTable.toString()).append("\n");
    }

    private Table buildStatisticsTable(ImmutableList<String> allDatabases, MutableMap<String, MutableMap<TestStatus, Integer>> statusesByDatabase)
    {
        Table.Builder tableBuilder = new Table.Builder();
        tableBuilder.withAlignment(Table.ALIGN_LEFT);

        ImmutableList<TestStatus> statuses = Lists.immutable.of(TestStatus.values());
        MutableList<String> headers = Lists.mutable.with("Database").withAll(statuses.collect(ts -> ts.friendlyName)).with("Total");
        tableBuilder.addRow(headers.toArray());

        for (String database : allDatabases)
        {
            ImmutableList<Integer> counts = statuses.collect(status -> statusesByDatabase.get(database).getOrDefault(status, 0));
            MutableList<Object> row = Lists.mutable.<Object>with(database).withAll(counts).with(counts.reduce(Integer::sum).orElse(0));
            tableBuilder.addRow(row.toArray());
        }

        return tableBuilder.build();
    }

    private Table buildTestDetailsTable(MutableMap<String, MutableMap<TestStatus, Integer>> statusesByDatabase)
    {
        ImmutableList<String> allDatabases = this.allDatabasesInSpecificOrder();
        MutableList<String> headers = Lists.mutable.with("Test Name").withAll(allDatabases);

        Table.Builder tableBuilder = new Table.Builder();
        tableBuilder.withAlignment(Table.ALIGN_LEFT);
        tableBuilder.addRow(headers.toArray());

        MutableSet<String> testNames = this.summaries.flatCollect(s -> s.testNames()).toSet();

        MutableMap<String, LegendDatabaseTestingReportGenerator.SQLTestSummary> summariesByDatabase = summaries.toMap(s -> resolveDatabaseName(s.database), s -> s);
        for (String testName : testNames)
        {
            ImmutableList<TestStatus> testResults = allDatabases.collect(db ->
                    {
                        LegendDatabaseTestingReportGenerator.SQLTestSummary emptySummary = new LegendDatabaseTestingReportGenerator.SQLTestSummary();
                        TestStatus testStatus = summariesByDatabase.getOrDefault(db, emptySummary).getTestResult(testName);

                        statusesByDatabase.putIfAbsent(db, Maps.mutable.empty());
                        statusesByDatabase.get(db).putIfAbsent(testStatus, 0);
                        statusesByDatabase.get(db).compute(testStatus, (ts, count) -> count + 1);

                        return testStatus;
                    }
            );

            MutableList<String> testResultsRow = Lists.mutable.with(testName).withAll(testResults.collect(ts -> ts.emoji));
            tableBuilder.addRow(testResultsRow.toArray());
        }
        return tableBuilder.build();
    }

    private Table buildTestLegendTable()
    {
        // add a header for every database
        MutableList<String> headers = Lists.mutable.with("Test Status", "Emoji", "Description");

        Table.Builder tableBuilder = new Table.Builder();
        tableBuilder.withAlignment(Table.ALIGN_LEFT);
        tableBuilder.addRow(headers.toArray());

        ArrayIterate.forEach(TestStatus.values(), status -> tableBuilder.addRow(status.friendlyName, status.emoji, status.description));
        return tableBuilder.build();
    }

    private static String UNKNOWN_DATABASE = "UNKNOWN_DATABASE";

    private String resolveDatabaseName(String database)
    {
        Optional<DatabaseType> databaseType = ArrayIterate.detectOptional(DatabaseType.values(), type -> type.name().equals(database));
        if (!databaseType.isPresent())
        {
            System.out.println("Report processing error. Unknown database type '" + database + "'");
            return UNKNOWN_DATABASE;
        }
        return databaseType.get().name();
    }

    private ImmutableList<String> allDatabasesInSpecificOrder()
    {
        ImmutableList<DatabaseType> allDatabaseTypes = Lists.immutable.with(DatabaseType.values());

        // databases that we have tests for OR databases that are actively being used by Legend users OR databases that have convenient test doubles or test infrastructure
        ImmutableList<DatabaseType> databaseSet1 = Lists.immutable.of(
                DatabaseType.H2, DatabaseType.Postgres,
                DatabaseType.SqlServer,
                DatabaseType.Snowflake, DatabaseType.Databricks,
                DatabaseType.BigQuery, DatabaseType.Spanner,
                DatabaseType.MemSQL, DatabaseType.Presto,
                DatabaseType.Redshift, DatabaseType.Athena);

        // everything else
        ImmutableList<DatabaseType> databaseSet2 = allDatabaseTypes.reject(d -> databaseSet1.contains(d));

        MutableList<DatabaseType> rearranged = Lists.mutable.withAll(databaseSet1);
        rearranged.addAllIterable(databaseSet2);

        //return rearranged.collect(Enum::name).collect(String::toLowerCase).toImmutable();
        return rearranged.collect(Enum::name).toImmutable();
    }
}
