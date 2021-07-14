package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift;

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;

import java.util.List;
import java.util.stream.Collectors;

public class RedshiftCommands extends RelationalDatabaseCommands
{
    @Override
    public String dropTempTable(String tableName)
    {
        return "drop table if exists " + tableName;
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

    @Override
    public <T> T accept(RelationalDatabaseCommandsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

}
