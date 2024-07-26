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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.Command;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;
import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.sql.Connection;
import java.sql.Statement;

public class DataCube__DEV__runDuckDBUpdateSQL implements Command
{
    private final DataCube parentCommand;
    private final Client client;

    public DataCube__DEV__runDuckDBUpdateSQL(DataCube parentCommand, Client client)
    {
        this.parentCommand = parentCommand;
        this.client = client;
    }

    @Override
    public String documentation()
    {
        return "datacube DEV__runDuckDBUpdateSQL -- <SQL expression>";
    }

    @Override
    public String description()
    {
        return "[DEV] run the UPDATE/INSERT SQL against DuckDB";
    }

    @Override
    public Command parentCommand()
    {
        return this.parentCommand;
    }

    @Override
    public boolean process(String line) throws Exception
    {
        if (line.startsWith("datacube DEV__runDuckDBUpdateSQL --"))
        {
            String[] tokens = line.split(" ");
            if (tokens.length <= 3)
            {
                throw new RuntimeException("Command should be used as '" + this.documentation() + "'");
            }

            int commandLength = "datacube DEV__runDuckDBUpdateSQL --".length() + 1;
            String expression = line.substring(commandLength).trim();
            DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(this.client.getModelState().parse(), DataCube.getLocalConnectionPath());

            try (Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor()))
            {
                try (Statement statement = connection.createStatement())
                {
                    statement.executeUpdate(expression);
                    this.client.printInfo("Executed UPDATE SQL: '" + expression + "'");
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
