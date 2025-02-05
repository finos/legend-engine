// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.providers;

import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.protocol.pure.m3.function.Lambda;
import org.finos.legend.engine.query.sql.providers.core.SQLSource;
import org.finos.legend.engine.query.sql.providers.core.SQLSourceArgument;
import org.finos.legend.engine.query.sql.providers.core.TableSource;
import org.finos.legend.engine.query.sql.providers.shared.AbstractLegendStoreSQLSourceProvider;
import org.finos.legend.engine.query.sql.providers.shared.project.ProjectCoordinateLoader;
import org.finos.legend.engine.query.sql.providers.shared.utils.SQLProviderUtils;

import java.util.Collections;
import java.util.List;

/**
 * This class serves for handling the **relationalStore** source type
 * <p>
 * Sample Select statement
 * select * from relationalStore(connection => 'my::Connection', store => 'my::Store', schema => 'schema1', table => 'table1', coordinates => 'com.gs:proj1:1.0.0')
 * select * from relationalStore(connection => 'my::Connection', store => 'my::Store', schema => 'schema1', table => 'table1', project => 'PROD-12345', workspace => 'myworkspace')
 * select * from relationalStore(connection => 'my::Connection', store => 'my::Store', schema => 'schema1', table => 'table1', project => 'PROD-12345', groupWorkspace => 'myworkspace')
 */
public class RelationalStoreSQLSourceProvider extends AbstractLegendStoreSQLSourceProvider<Database>
{

    private static final String TYPE = "relationalStore";
    private static final String ARG_SCHEMA = "schema";
    private static final String ARG_TABLE = "table";

    public RelationalStoreSQLSourceProvider(ProjectCoordinateLoader projectCoordinateLoader)
    {
        super(Database.class, projectCoordinateLoader);
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    protected SQLSource createSource(TableSource source, Database store, PackageableConnection connection, List<SQLSourceArgument> keys, PureModelContextData pmcd)
    {
        String schemaName = source.getArgumentValueAs(ARG_SCHEMA, -1, String.class, true);
        String tableName = source.getArgumentValueAs(ARG_TABLE, -1, String.class, true);

        Lambda lambda = tableToTDS(store, schemaName, tableName);
        EngineRuntime runtime = SQLProviderUtils.createRuntime(connection.getPath(), store.getPath());

        Collections.addAll(keys, new SQLSourceArgument(ARG_SCHEMA, null, schemaName), new SQLSourceArgument(ARG_TABLE, null, tableName));

        return new SQLSource(TYPE, lambda, null, runtime, null, null, keys);
    }


    protected static Lambda tableToTDS(Database database, String schemaName, String tableName)
    {
        Schema schema = SQLProviderUtils.extractElement("schema", database.schemas, s -> SQLProviderUtils.equalsEscaped(s.name, schemaName));
        Table table = SQLProviderUtils.extractElement("table", schema.tables, t -> SQLProviderUtils.equalsEscaped(t.name, tableName));

        return SQLProviderUtils.tableToTDS(database.getPath(), schema.name, table.name);
    }
}