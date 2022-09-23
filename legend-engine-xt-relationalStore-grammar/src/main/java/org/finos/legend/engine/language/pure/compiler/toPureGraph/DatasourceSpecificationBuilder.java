// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatabricksDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.EmbeddedH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.RedshiftDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatabricksDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatabricksDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_EmbeddedH2DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_EmbeddedH2DatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_LocalH2DatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_LocalH2DatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_StaticDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_alloy_connections_alloy_specification_StaticDatasourceSpecification_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification;
import org.finos.legend.pure.generated.Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification_Impl;


public class DatasourceSpecificationBuilder implements DatasourceSpecificationVisitor<Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification>
{
    private final CompileContext context;

    public DatasourceSpecificationBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public Root_meta_pure_alloy_connections_alloy_specification_DatasourceSpecification visit(DatasourceSpecification datasourceSpecification)
    {
        if (datasourceSpecification instanceof LocalH2DatasourceSpecification)
        {
            LocalH2DatasourceSpecification localH2DatasourceSpecification = (LocalH2DatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_LocalH2DatasourceSpecification local = new Root_meta_pure_alloy_connections_alloy_specification_LocalH2DatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::LocalH2DatasourceSpecification"));
            local._testDataSetupCsv(localH2DatasourceSpecification.testDataSetupCsv);
            local._testDataSetupSqls(localH2DatasourceSpecification.testDataSetupSqls == null ? FastList.newList() : FastList.newList(localH2DatasourceSpecification.testDataSetupSqls));
            return local;
        }
        else if (datasourceSpecification instanceof EmbeddedH2DatasourceSpecification)
        {
            EmbeddedH2DatasourceSpecification embeddedH2DatasourceSpecification = (EmbeddedH2DatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_EmbeddedH2DatasourceSpecification embedded = new Root_meta_pure_alloy_connections_alloy_specification_EmbeddedH2DatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::EmbeddedH2DatasourceSpecification"));
            embedded._autoServerMode(embeddedH2DatasourceSpecification.autoServerMode);
            embedded._databaseName(embeddedH2DatasourceSpecification.databaseName);
            embedded._directory(embeddedH2DatasourceSpecification.directory);
            return embedded;
        }
        else if (datasourceSpecification instanceof StaticDatasourceSpecification)
        {
            StaticDatasourceSpecification staticDatasourceSpecification = (StaticDatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_StaticDatasourceSpecification _static = new Root_meta_pure_alloy_connections_alloy_specification_StaticDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::StaticDatasourceSpecification"));
            _static._host(staticDatasourceSpecification.host);
            _static._port(staticDatasourceSpecification.port);
            _static._databaseName(staticDatasourceSpecification.databaseName);
            return _static;
        }
        else if (datasourceSpecification instanceof DatabricksDatasourceSpecification)
        {
            DatabricksDatasourceSpecification staticDatasourceSpecification = (DatabricksDatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_DatabricksDatasourceSpecification _static = new Root_meta_pure_alloy_connections_alloy_specification_DatabricksDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::DatabricksDatasourceSpecification"));
            _static._hostname(staticDatasourceSpecification.hostname);
            _static._port(staticDatasourceSpecification.port);
            _static._protocol(staticDatasourceSpecification.protocol);
            _static._httpPath(staticDatasourceSpecification.httpPath);
            return _static;
        }
        else if (datasourceSpecification instanceof SnowflakeDatasourceSpecification)
        {
            SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = (SnowflakeDatasourceSpecification) datasourceSpecification;
            Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification _snowflake = new Root_meta_pure_alloy_connections_alloy_specification_SnowflakeDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::alloy::connections::alloy::specification::SnowflakeDatasourceSpecification"));
            _snowflake._accountName(snowflakeDatasourceSpecification.accountName);
            _snowflake._region(snowflakeDatasourceSpecification.region);
            _snowflake._warehouseName(snowflakeDatasourceSpecification.warehouseName);
            _snowflake._databaseName(snowflakeDatasourceSpecification.databaseName);
            _snowflake._cloudType(snowflakeDatasourceSpecification.cloudType);
            _snowflake._quotedIdentifiersIgnoreCase(snowflakeDatasourceSpecification.quotedIdentifiersIgnoreCase);
            _snowflake._proxyHost(snowflakeDatasourceSpecification.proxyHost);
            _snowflake._proxyPort(snowflakeDatasourceSpecification.proxyPort);
            _snowflake._nonProxyHosts(snowflakeDatasourceSpecification.nonProxyHosts);
            if (snowflakeDatasourceSpecification.accountType != null)
            {
                _snowflake._accountType(this.context.pureModel.getEnumValue("meta::pure::alloy::connections::alloy::specification::SnowflakeAccountType", snowflakeDatasourceSpecification.accountType));
            }
            _snowflake._organization(snowflakeDatasourceSpecification.organization);
            _snowflake._role(snowflakeDatasourceSpecification.role);

            return _snowflake;
        }
        else if (datasourceSpecification instanceof RedshiftDatasourceSpecification)
        {
            RedshiftDatasourceSpecification redshiftDatasourceSpecification = (RedshiftDatasourceSpecification) datasourceSpecification;
            Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification redshiftSpec = new Root_meta_pure_legend_connections_legend_specification_RedshiftDatasourceSpecification_Impl("", null, context.pureModel.getClass("meta::pure::legend::connections::legend::specification::RedshiftDatasourceSpecification"));
            redshiftSpec._clusterID(redshiftDatasourceSpecification.clusterID);
            redshiftSpec._databaseName(redshiftDatasourceSpecification.databaseName);
            redshiftSpec._endpointURL(redshiftDatasourceSpecification.endpointURL);
            redshiftSpec._host(redshiftDatasourceSpecification.host);
            redshiftSpec._port(redshiftDatasourceSpecification.port);
            redshiftSpec._region(redshiftDatasourceSpecification.region);
            return redshiftSpec;
        }
        return null;
    }
}