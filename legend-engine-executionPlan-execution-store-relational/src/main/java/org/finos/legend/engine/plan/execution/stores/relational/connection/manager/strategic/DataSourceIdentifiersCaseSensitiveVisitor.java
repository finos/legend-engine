package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;

public class DataSourceIdentifiersCaseSensitiveVisitor implements DatasourceSpecificationVisitor<Boolean>
{
    public Boolean visit(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) datasourceSpecification;
            return snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase == null || !snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase;
        }
        else if (datasourceSpecification instanceof RedshiftDatasourceSpecification)
        {
//            documentation link : https://docs.aws.amazon.com/redshift/latest/dg/r_names.html
            return false;
        }
        return null;
    }
}
