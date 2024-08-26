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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.finos.legend.engine.repl.relational.shared.ResultHelper.prettyGridPrint;

public class DataCube__DEV__runDuckDBSelectSQL implements Command
{
    private final DataCube parentCommand;
    private final Client client;

    public DataCube__DEV__runDuckDBSelectSQL(DataCube parentCommand, Client client)
    {
        this.parentCommand = parentCommand;
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "datacube DEV__runDuckDBSelectSQL -- <SQL expression>";
    }

    @Override
    public String description()
    {
        return "[DEV] run the SELECT SQL against DuckDB";
    }

    @Override
    public Command parentCommand()
    {
        return this.parentCommand;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("datacube DEV__runDuckDBSelectSQL --"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length <= 3)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            int commandLength = "datacube DEV__runDuckDBSelectSQL --".length() + 1;
            String expression = line.substring(commandLength).trim();
            DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), DataCube.getLocalConnectionPath());

            try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
            {
                try (Statement statement = connection.createStatement())
                {
                    ResultSet result = statement.executeQuery(expression);
                    List<String> columnNames = Lists.mutable.empty();
                    List<String> columnTypes = Lists.mutable.empty();
                    for (int i = 1; i <= result.getMetaData().getColumnCount(); i++)
                    {
                        columnNames.add(result.getMetaData().getColumnLabel(i));
                        columnTypes.add(result.getMetaData().getColumnTypeName(i));
                    }
                    this.client.println("Executed SELECT SQL: '" + expression + "'");
                    this.client.println(prettyGridPrint(result, columnNames, columnNames, 40, 60));
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public MutableList<Candidate> complete(String inScope, LineReader lineReader, ParsedLine parsedLine)
    {
        return null;
    }
}
