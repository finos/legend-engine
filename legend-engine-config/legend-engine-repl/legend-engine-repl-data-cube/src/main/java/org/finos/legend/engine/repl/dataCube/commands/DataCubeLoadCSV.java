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

package org.finos.legend.engine.repl.dataCube.commands;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.client.jline3.JLine3Parser;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.dataCube.server.REPLServer;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.jline.builtins.Completers;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_SIGNATURE;

public class DataCubeLoadCSV implements Command
{
    private final DataCube parentCommand;
    private final Client client;
    private final REPLServer replServer;
    private final Completers.FilesCompleter completer = new Completers.FilesCompleter(new File("/"));

    public DataCubeLoadCSV(DataCube parentCommand, Client client, REPLServer replServer)
    {
        this.parentCommand = parentCommand;
        this.client = client;
        this.replServer = replServer;
    }

    @Override
    public String documentation()
    {
        return "datacube load_csv <path> <table name>";
    }

    @Override
    public String description()
    {
        return "load CSV into the specified table then launch DataCube with that table";
    }

    @Override
    public Command parentCommand()
    {
        return this.parentCommand;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("datacube load_csv"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length != 4)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            String tableName = tokens[3];
            DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), DataCube.getLocalConnectionPath());
            try (
                    Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor());
                    Statement statement = connection.createStatement())
            {
                statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().load(tableName, tokens[2]));
                this.client.println("Loaded into table: '" + tableName + "'");
            }

            String functionBodyCode = "#>{" + DataCube.getLocalDatabasePath() + "." + tableName + "}#->from(" + DataCube.getLocalRuntimePath() + ")";
            String functionCode = "###Pure\n" +
                    "function " + REPL_RUN_FUNCTION_SIGNATURE + "\n{\n" + functionBodyCode + ";\n}";
            PureModelContextData pureModelContextData = client.getModelState().parseWithTransient(functionCode);
            this.replServer.initializeStateFromTable(pureModelContextData);
            Show.launchDataCube(client, replServer);
            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        if (StringUtils.stripStart(inScope, null).startsWith("datacube load_csv"))
        {
            MutableList<String> words = Lists.mutable.withAll(parsedLine.words()).drop(3);
            String compressed = StringUtils.stripStart(words.makeString(""), null);
            MutableList<Candidate> list = Lists.mutable.empty();
            completer.complete(lineReader, new JLine3Parser.MyParsedLine(new JLine3Parser.ParserResult(parsedLine.line(), Lists.mutable.with(compressed))), list);
            MutableList<Candidate> ca = ListIterate.collect(list, c ->
            {
                String val = compressed.length() == 1 ? c.value() : c.value().substring(1);
                return new Candidate(val, val, null, null, null, null, false, 0);
            });
            list.clear();
            list.addAll(ca);
            return list;
        }
        return null;
    }
}
