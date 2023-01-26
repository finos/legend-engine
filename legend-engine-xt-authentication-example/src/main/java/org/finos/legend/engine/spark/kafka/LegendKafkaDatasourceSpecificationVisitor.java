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

package org.finos.legend.engine.spark.kafka;

import org.apache.spark.sql.DataFrameReader;
import org.finos.legend.engine.connection.kafka.KafkaDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecificationVisitor;

public class LegendKafkaDatasourceSpecificationVisitor implements DatasourceSpecificationVisitor<KafkaDatasourceSpecification>
{
    private DataFrameReader dataFrameReader;

    public LegendKafkaDatasourceSpecificationVisitor(DataFrameReader dataFrameReader)
    {
        this.dataFrameReader = dataFrameReader;
    }

    @Override
    public KafkaDatasourceSpecification visit(DatasourceSpecification datasourceSpecification)
    {
        KafkaDatasourceSpecification kafkaDatasourceSpecification = (KafkaDatasourceSpecification) datasourceSpecification;
        this.dataFrameReader
                .option("kafka.bootstrap.servers", kafkaDatasourceSpecification.bootstrapBrokerList)
                .option("subscribe", kafkaDatasourceSpecification.topic)
                .option("startingOffsets", "earliest")
                .option("endingOffsets", "latest");
        return null;
    }
}
