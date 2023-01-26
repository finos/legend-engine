// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.spark;

import org.apache.spark.sql.DataFrameReader;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.connection.ConnectionSpecification;
import org.finos.legend.engine.connection.ConnectionSpecificationProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.spark.kafka.LegendKafkaDatasourceSpecificationVisitor;

import java.util.Optional;

public class LegendSparkDataFrameConfigurator
{
    private ImmutableList<String> supportedDataSourceTypes = Lists.immutable.of("kafka");

    private ConnectionSpecificationProvider connectionSpecificationProvider;

    public LegendSparkDataFrameConfigurator(ConnectionSpecificationProvider connectionSpecificationProvider)
    {
        this.connectionSpecificationProvider = connectionSpecificationProvider;
    }

    public DataFrameReader addResource(String format, String resourceName, DataFrameReader dataFrameReader)
    {
        if (!this.supportedDataSourceTypes.contains(format))
        {
            throw new RuntimeException(String.format("Configuration exception. Unsupported format. Format='%s', Resource='%s'", format, resourceName));
        }
        Optional<ConnectionSpecification> holder = this.connectionSpecificationProvider.get(resourceName);
        if (!holder.isPresent())
        {
            throw new RuntimeException(String.format("Configuration exception. Unknown resource. Format='%s', Resource='%s'", format, resourceName));
        }
        ConnectionSpecification connectionSpecification = holder.get();
        DatasourceSpecification datasourceSpecification = connectionSpecification.datasourceSpecification;
        datasourceSpecification.accept(new LegendKafkaDatasourceSpecificationVisitor(dataFrameReader));
        return dataFrameReader;
    }
}
