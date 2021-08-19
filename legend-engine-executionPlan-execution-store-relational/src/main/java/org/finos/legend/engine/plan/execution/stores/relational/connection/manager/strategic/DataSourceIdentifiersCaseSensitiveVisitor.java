package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;

public class DataSourceIdentifiersCaseSensitiveVisitor implements DatasourceSpecificationVisitor<Boolean>
{
    public Boolean visit(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            //SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) datasourceSpecification;
            //return snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase == null || !snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase;
            // TOOD : what to do here
            return false;
        }
        return null;
    }
}
