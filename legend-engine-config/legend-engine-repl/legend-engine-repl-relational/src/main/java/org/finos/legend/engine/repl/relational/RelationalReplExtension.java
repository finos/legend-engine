// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.relational;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.core.ReplExtension;
import org.finos.legend.engine.repl.core.commands.Execute;
import org.finos.legend.engine.repl.relational.commands.Cache;
import org.finos.legend.engine.repl.relational.commands.DB;
import org.finos.legend.engine.repl.relational.commands.Drop;
import org.finos.legend.engine.repl.relational.commands.Load;
import org.finos.legend.engine.repl.relational.local.LocalConnectionManagement;
import org.finos.legend.engine.repl.relational.local.LocalConnectionType;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.jline.jansi.Ansi.ansi;

public class RelationalReplExtension implements ReplExtension
{
    private Client client;
    public static String DUCKDB_LOCAL_CONNECTION_BASE_NAME = "DuckDuck";
    public static String CACHED_SERIALIZED_RESULTS_DIR = "relational/cachedResults";

    private LocalConnectionManagement localConnectionManagement;

    static
    {
        int port = 1024 + (int) (Math.random() * 10000);
        System.setProperty("legend.test.h2.port", String.valueOf(port));
        try
        {
            AlloyH2Server.startServer(port);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String type()
    {
        return "relational";
    }

    private boolean canShowGrid()
    {
        return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }

    public void initialize(Client client)
    {
        this.client = client;
        this.localConnectionManagement = new LocalConnectionManagement(client);
        //this.localConnectionManagement.addLocalConnection(LocalConnectionType.H2, "MyTestH2");
        this.localConnectionManagement.addLocalConnection(LocalConnectionType.DuckDB, DUCKDB_LOCAL_CONNECTION_BASE_NAME);

        try
        {
            flushCachedResults(this.client);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void flushCachedResults(Client client)
    {
        try
        {
            File cachedResultDir = client.getHomeDir().resolve(CACHED_SERIALIZED_RESULTS_DIR).toFile();
            if (cachedResultDir.exists())
            {
                FileUtils.cleanDirectory(cachedResultDir);
            }
            else
            {
                Files.createDirectories(cachedResultDir.toPath());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Path getCachedSerializedResultPath(String serializationId, Client client)
    {
        return client.getHomeDir().resolve(CACHED_SERIALIZED_RESULTS_DIR).resolve(serializationId + ".csv");
    }

    @Override
    public MutableList<String> generateDynamicContent(String code)
    {
        return localConnectionManagement.generateDynamicContent(code);
    }

    @Override
    public MutableList<Command> getExtraCommands()
    {
        return Lists.mutable.with(
                new DB(this.client, this),
                new Load(this.client, this),
                new Drop(this.client),
                new Cache(this.client, this.client.getPlanExecutor())
        );
    }

    @Override
    public boolean supports(Result res)
    {
        return res instanceof RelationalResult;
    }

    @Override
    public String print(Result res)
    {
        return printAndSerializeResultToCSV((RelationalResult) res, 40, 60);
    }

    // TODO: the return of this will be printed directly to the console, so we should be mindful of the size
    // in order to not flood the console, and making client wait for the print to finish before moving on to next operation
    public String printAndSerializeResultToCSV(RelationalResult res, int maxRowSize, int maxColSize)
    {
        String serializationFileName = ((Execute) client.commands.getLast()).getCurrentExecutionId();
        serializationFileName = serializationFileName != null ? serializationFileName : UUID.randomUUID().toString();
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(res.getColumnListForSerializer().toArray(new String[0]));
        flushCachedResults(client);
        Path filePath = getCachedSerializedResultPath(serializationFileName, client);

        MutableList<String> columns = Lists.mutable.empty();
        MutableList<Integer> size = Lists.mutable.empty();
        MutableList<MutableList<String>> values = Lists.mutable.empty();

        try (
                ResultSet rs = res.resultSet;
                FileOutputStream outputStream = new FileOutputStream(filePath.toString());
                Writer fileWriter = new BufferedWriter(new OutputStreamWriter(outputStream));
                CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat))
        {
            int columnCount = res.sqlColumns.size();
            for (int i = 0; i < columnCount; i++)
            {
                columns.add(res.sqlColumns.get(i));
                values.add(Lists.mutable.empty());
            }

            while (rs.next())
            {
                for (int i = 1; i <= columnCount; i++)
                {
                    String value = rs.getObject(i) == null ? "" : rs.getObject(i).toString();
                    csvPrinter.print(value);
                    values.get(i - 1).add(value);
                }
                csvPrinter.println();
            }
            for (int i = 0; i < columnCount; i++)
            {
                size.add(values.get(i).injectInto(columns.get(i).length(), (IntObjectToIntFunction<? super String>) (a, b) -> Math.max(b.length(), a)));
            }
            size = Lists.mutable.withAll(size.collect(s -> Math.min(maxColSize, s + 2)));

            StringBuilder builder = new StringBuilder();

            drawSeparation(builder, columnCount, size);
            drawRow(builder, columnCount, size, columns::get, maxColSize);
            drawSeparation(builder, columnCount, size);

            int rows = values.get(0).size();
            for (int k = 0; k < rows; k++)
            {
                final int fk = k;
                drawRow(builder, columnCount, size, i -> values.get(i).get(fk), maxColSize);
            }

            drawSeparation(builder, columnCount, size);

            // add summary
            builder.append(ansi().fgBrightBlack().a(rows + " rows -- " + columns.size() + " columns").reset());
            return builder.toString();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void drawSeparation(StringBuilder builder, int count, MutableList<Integer> size)
    {
        builder.append("+");
        for (int i = 0; i < count; i++)
        {
            repeat('-', size.get(i), builder);
            builder.append("+");
        }
        builder.append("\n");
    }

    private static void repeat(char value, int length, StringBuilder builder)
    {
        for (int k = 0; k < length; k++)
        {
            builder.append(value);
        }
    }

    private static void drawRow(StringBuilder builder, int count, MutableList<Integer> size, Function<Integer, String> getter, int maxColSize)
    {
        builder.append("|");
        for (int i = 0; i < count; i++)
        {
            String val = printValue(getter.apply(i), maxColSize);
            int space = (size.get(i) - val.length()) / 2;
            repeat(' ', space, builder);
            builder.append(val);
            repeat(' ', size.get(i) - val.length() - space, builder);
            builder.append("|");
        }

        builder.append("\n");
    }

    private static String printValue(String str, int max)
    {
        return str.length() >= max ? str.substring(0, max - 3 - 2) + "..." : str;
    }
}
