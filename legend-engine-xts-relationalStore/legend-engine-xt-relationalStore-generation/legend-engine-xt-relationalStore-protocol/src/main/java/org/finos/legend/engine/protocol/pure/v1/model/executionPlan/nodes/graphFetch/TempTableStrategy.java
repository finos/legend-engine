// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.graphFetch;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, defaultImpl = TempTableStrategy.class, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = LoadFromSubQueryTempTableStrategy.class, name = "subQuery"),
        @JsonSubTypes.Type(value = LoadFromResultSetAsValueTuplesTempTableStrategy.class, name = "resultSet"),
        @JsonSubTypes.Type(value = LoadFromTempFileTempTableStrategy.class, name = "tempFile")
})
public class TempTableStrategy
{
    public ExecutionNode createTempTableNode;
    public ExecutionNode loadTempTableNode;
    public ExecutionNode dropTempTableNode;
}
