package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification;

public class DeltaLakeDatasourceSpecification extends DatasourceSpecification
{
    public String shard;
    public String httpPath;
    public String token;

    public Boolean quotedIdentifiersIgnoreCase;

    @Override
    public <T> T accept(DatasourceSpecificationVisitor<T> datasourceSpecificationVisitor)
    {
        return datasourceSpecificationVisitor.visit(this);
    }
}
