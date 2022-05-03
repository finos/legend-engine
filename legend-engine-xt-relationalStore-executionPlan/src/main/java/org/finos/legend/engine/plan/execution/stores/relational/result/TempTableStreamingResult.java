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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.finos.legend.engine.plan.execution.stores.relational.serialization.StreamingTempTableResultCSVSerializer;

import org.finos.legend.engine.plan.execution.result.ResultVisitor;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.result.builder.Builder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.result.serialization.Serializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.CreateAndPopulateTempTableExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.TempTableColumnMetaData;

import java.util.List;
import java.util.stream.Stream;

public class TempTableStreamingResult extends StreamingResult
{
    public Stream inputStream;
    public CreateAndPopulateTempTableExecutionNode node;
    public List<TempTableColumnMetaData> tempTableColumnMetaData;

    public TempTableStreamingResult(Stream inputStream, CreateAndPopulateTempTableExecutionNode node)
    {
        super(null);
        this.inputStream = inputStream;
        this.node = node;
        this.tempTableColumnMetaData = node.tempTableColumnMetaData;
    }

    @Override
    public <T> T accept(ResultVisitor<T> resultVisitor)
    {
        return ((RelationalResultVisitor<T>) resultVisitor).visit(this);
    }

    @Override
    public Builder getResultBuilder()
    {
        return null;
    }

    public String getRelationalDatabaseTimeZone()
    {
        return this.node.getDatabaseTimeZone();
    }

    @Override
    public Serializer getSerializer(SerializationFormat format)
    {
        if (format.equals(SerializationFormat.DEFAULT))
        {
            return new StreamingTempTableResultCSVSerializer(this, true);
        }
        else
        {
            throw new RuntimeException(format.toString() + " format not currently supported with TempTableStreamingResult");
        }
    }
}
