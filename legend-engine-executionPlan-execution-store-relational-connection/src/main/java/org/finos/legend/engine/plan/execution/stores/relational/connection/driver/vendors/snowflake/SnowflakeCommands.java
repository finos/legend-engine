package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;

import java.util.List;
import java.util.stream.Collectors;

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
        return Lists.mutable.with(
                "USE SCHEMA TPCDS_SF100TCL;"+
                    "CREATE LOCAL TEMP TABLE IF NOT EXISTS " + tableName + "(" + columns.stream().map(c -> c.name + " " + c.type).collect(Collectors.joining(", ")) + ");" +
                    "PUT file://" + optionalCSVFileLocation + " @ALLOY_TESTING_DB.TPCDS_SF100TCL.%"+tableName+" PARALLEL= 16 AUTO_COMPRESS=TRUE;" +
                    "COPY INTO " + tableName + " FROM @ALLOY_TESTING_DB.TPCDS_SF100TCL.%" + tableName + " file_format = (type= CSV field_optionally_enclosed_by = '\"') on_error = 'skip_file';");
    }

    @Override
    public IngestionMethod getDefaultIngestionMethod()
    {
        return IngestionMethod.CLIENT_FILE;
    }

    @Override
    public <T> T accept(RelationalDatabaseCommandsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

}
