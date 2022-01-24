package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification;

public class SnowflakeDatasourceSpecification extends DatasourceSpecification
{
    public String accountName;
    public String region;
    public String warehouseName;
    public String databaseName;
    public String cloudType;

    public Boolean quotedIdentifiersIgnoreCase;
    public String proxyHost;
    public String proxyPort;
    public String nonProxyHosts;
    public String organization;
    public String accountType;

    public String role;

    @Override
    public <T> T accept(DatasourceSpecificationVisitor<T> datasourceSpecificationVisitor)
    {
        return datasourceSpecificationVisitor.visit(this);
    }
}
