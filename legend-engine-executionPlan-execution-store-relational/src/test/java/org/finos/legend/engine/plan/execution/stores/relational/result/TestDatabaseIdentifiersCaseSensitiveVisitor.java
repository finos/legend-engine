package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.junit.Assert;
import org.junit.Test;

public class TestDatabaseIdentifiersCaseSensitiveVisitor
{
    @Test
    public void testDatabaseIdentifiersCaseSensitiveVisitorDefaultsToTrue()
    {
        RelationalDatabaseConnection databaseConnection = new RelationalDatabaseConnection();
        databaseConnection.datasourceSpecification = new LocalH2DatasourceSpecification();
        boolean result = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertTrue(result);
    }

    @Test
    public void testDatabaseIdentifiersCaseSensitiveVisitorForSnowflakeDatasourceSpecification()
    {
        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        RelationalDatabaseConnection databaseConnection = new RelationalDatabaseConnection();

        databaseConnection.datasourceSpecification = snowflakeDatasourceSpecification;
        boolean resultWithFlagNotSet = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertTrue(resultWithFlagNotSet);

        snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase = false;
        boolean resultWithFlagSetAsFalse = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertTrue(resultWithFlagSetAsFalse);

        snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase = true;
        boolean resultWithFlagSetAsTrue = databaseConnection.accept(new DatabaseIdentifiersCaseSensitiveVisitor());
        Assert.assertFalse(resultWithFlagSetAsTrue);
    }
}
