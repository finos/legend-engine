package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.BigQueryDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;

import java.util.List;

public class BigQueryCompilerExtension implements IRelationalCompilerExtension
{
    @Override
    public List<Function2<DatasourceSpecification, CompileContext, Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>> getExtraDataSourceSpecificationProcessors()
    {
        return Lists.mutable.with((datasourceSpecification, context) ->
        {
            if (datasourceSpecification instanceof BigQueryDatasourceSpecification)
            {
                BigQueryDatasourceSpecification bigQueryDatasourceSpecification = (BigQueryDatasourceSpecification) datasourceSpecification;
                Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification _bigquery = new Root_meta_pure_alloy_connections_alloy_specification_BigQueryDatasourceSpecification_Impl("");
                _bigquery._projectId(bigQueryDatasourceSpecification.projectId);
                _bigquery._defaultDataset(bigQueryDatasourceSpecification.defaultDataset);
                _bigquery._proxyHost(bigQueryDatasourceSpecification.proxyHost);
                _bigquery._proxyPort(bigQueryDatasourceSpecification.proxyPort);
                return _bigquery;
            }
            return null;
        });
    }
}
