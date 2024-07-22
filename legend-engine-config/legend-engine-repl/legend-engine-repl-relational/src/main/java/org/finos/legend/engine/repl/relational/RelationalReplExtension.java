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

import org.apache.commons.io.FileUtils;
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
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.finos.legend.engine.repl.relational.shared.ResultHelper.printAndSerializeResultSetToCSV;

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
        RelationalResult relationalResult = (RelationalResult) res;
        String serializationFileName = ((Execute) client.commands.getLast()).getCurrentExecutionId();
        serializationFileName = serializationFileName != null ? serializationFileName : UUID.randomUUID().toString();
        flushCachedResults(client);
        Path filePath = getCachedSerializedResultPath(serializationFileName, client);

        try (
                ResultSet rs = relationalResult.resultSet;
                FileOutputStream outputStream = new FileOutputStream(filePath.toString());
        )
        {
            return printAndSerializeResultSetToCSV(rs, relationalResult.sqlColumns, relationalResult.getColumnListForSerializer(), outputStream, 40, 60);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
