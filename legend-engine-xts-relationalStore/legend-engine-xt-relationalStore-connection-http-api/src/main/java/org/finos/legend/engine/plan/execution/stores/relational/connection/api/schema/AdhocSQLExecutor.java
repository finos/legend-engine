// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.api.schema;

import com.opencsv.CSVWriter;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AdhocSQLExecutor
{
    private static final int PREVIEW_LIMIT = 1000;

    public String executeRawSQL(ConnectionManagerSelector connectionManager, RelationalDatabaseConnection conn, String sqlQuery, Identity identity) throws SQLException, IOException
    {
        try (Connection connection = connectionManager.getDatabaseConnection(identity, conn))
        {
            Statement stmt = connection.createStatement();
            stmt.setMaxRows(PREVIEW_LIMIT);
            ResultSet resultSet = stmt.executeQuery(sqlQuery);
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            csvWriter.writeAll(resultSet, true, true, false);
            return stringWriter.toString();
        }
    }
}
