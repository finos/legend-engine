package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection;

import java.util.List;

public class FinCloudDatasourceSpecification extends FinCloudTargetSpecification{
    public String apiUrl;

    public FinCloudDatasourceSpecification()
    {
    }

    public FinCloudDatasourceSpecification(String testDataSetupCsv, List<String> testDataSetupSqls)
    {
        this.apiUrl = apiUrl;
    }

    @Override
    public <T> T accept(FinCloudTargetSpecificationVisitor<T> finCloudTargetSpecificationVisitor)
    {
        return finCloudTargetSpecificationVisitor.visit(this);
    }
}
