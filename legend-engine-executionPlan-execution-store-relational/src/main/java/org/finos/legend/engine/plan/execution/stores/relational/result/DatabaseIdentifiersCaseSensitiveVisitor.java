package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.DataSourceIdentifiersCaseSensitiveVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;

@Deprecated
public class DatabaseIdentifiersCaseSensitiveVisitor implements ConnectionVisitor<Boolean>
{
    @Override
    public Boolean visit(Connection connection)
    {
        if (connection instanceof DatabaseConnection)
        {
            //By default Legend expects DatabaseIdentifiers to be case-sensitive
            if (connection instanceof RelationalDatabaseConnection)
            {
                DatasourceSpecificationVisitor<Boolean> datasourceSpecificationVisitor = new DataSourceIdentifiersCaseSensitiveVisitor();
                Boolean isDatabaseIdentifiersCaseSensitive = ((RelationalDatabaseConnection) connection).datasourceSpecification.accept(datasourceSpecificationVisitor);
                return isDatabaseIdentifiersCaseSensitive != null ? isDatabaseIdentifiersCaseSensitive : true;
            }
            else
            {
                return true;
            }
        }
        else
        {
            return throwNotSupportedException(connection);
        }
    }

    @Override
    public Boolean visit(ConnectionPointer connectionPointer)
    {
        return throwNotSupportedException(connectionPointer);
    }

    @Override
    public Boolean visit(ModelConnection modelConnection)
    {
        return throwNotSupportedException(modelConnection);
    }

    @Override
    public Boolean visit(JsonModelConnection jsonModelConnection)
    {
        return throwNotSupportedException(jsonModelConnection);
    }

    @Override
    public Boolean visit(XmlModelConnection xmlModelConnection)
    {
        return throwNotSupportedException(xmlModelConnection);
    }

    @Override
    public Boolean visit(ModelChainConnection modelChainConnection)
    {
        return throwNotSupportedException(modelChainConnection);
    }

    private static Boolean throwNotSupportedException(Connection connection)
    {
        throw new RuntimeException("DatabaseIdentifiersCaseSensitiveVisitor supports DatabaseConnection only. Found - " + connection.getClass().getSimpleName());
    }
}
