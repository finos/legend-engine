package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

//import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;

import java.sql.Connection;
import java.util.List;

public class SnowflakeCommands extends RelationalDatabaseCommands
{
    @Override
    public String dropTempTable(String tableName)
    {
        return "Drop table if exists " + tableName;
    }

    @Override
    public List<String> createAndLoadTempTable(String tableName, List<Column> columns, String optionalCSVFileLocation)
    {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public IngestionMethod getDefaultIngestionMethod()
    {
        throw new UnsupportedOperationException("not yet implemented");
    }

/*    @Override
    protected void buildTempTableFromResult(RelationalExecutionConfiguration config, Connection connection, StreamingResult result, String tableName, IngestionMethod ingestionMethod, String databaseTimeZone) {
        if (ingestionMethod == null)
        {
            ingestionMethod = this.getDefaultIngestionMethod();
        }
        throw new UnsupportedOperationException("not yet implemented");
    }*/
}
