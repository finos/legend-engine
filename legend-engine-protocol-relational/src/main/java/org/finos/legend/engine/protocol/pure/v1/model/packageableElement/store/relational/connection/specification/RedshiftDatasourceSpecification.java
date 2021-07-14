package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.Region;

public class RedshiftDatasourceSpecification extends DatasourceSpecification
{
    public String clusterName;
    public String clusterID;
    public String databaseName;
    public int port;
    public Region region;

    @Override
    public <T> T accept(DatasourceSpecificationVisitor<T> datasourceSpecificationVisitor)
    {
        return datasourceSpecificationVisitor.visit(this);
    }
}
