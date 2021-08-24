package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification;

public class SnowflakeDatasourceSpecification extends DatasourceSpecification
{
    public String accountName;
    public String region;
    public String warehouseName;
    public String databaseName;
    public String cloudType;

    public Boolean quotedIdentifiersIgnoreCase;

    @Override
    public <T> T accept(DatasourceSpecificationVisitor<T> datasourceSpecificationVisitor)
    {
        return datasourceSpecificationVisitor.visit(this);
    }

    @Override
    public String getKey() {
        return "Snowflake_" +
                "account:" + accountName + "_" +
                "region:" + region + "_" +
                "warehouse:" + warehouseName + "_" +
                "db:" + databaseName + "_" +
                "cloudType:" + cloudType + "_" +
                "quoteIdentifiers:" + quotedIdentifiersIgnoreCase;
    }
}
