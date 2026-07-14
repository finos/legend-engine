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

package org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;

import java.util.Collections;
import java.util.List;

public class CreateAndPopulateTempTableExecutionNode extends ExecutionNode
{
    @JsonAlias({"inputVarName", "inputVarNames"})
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<String> inputVarNames;
    public String tempTableName;
    public List<TempTableColumnMetaData> tempTableColumnMetaData = Collections.EMPTY_LIST;
    public DatabaseConnection connection;

    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit(this);
    }

    @JsonIgnore
    public String getDatabaseTimeZone()
    {
        return this.connection.timeZone;
    }

    /**
     * Returns a shallow copy of this node with the {@code connection} field replaced by
     * the supplied value. All other fields (including inherited {@link ExecutionNode}
     * fields) are copied by reference. Used by the relational executor to enrich a
     * plan-level connection with identity/allocation-derived state without mutating
     * the shared, cached plan.
     */
    @JsonIgnore
    public CreateAndPopulateTempTableExecutionNode shallowCopyWithConnection(DatabaseConnection newConnection)
    {
        CreateAndPopulateTempTableExecutionNode copy = new CreateAndPopulateTempTableExecutionNode();
        // ExecutionNode fields
        copy.resultType = this.resultType;
        copy.executionNodes = this.executionNodes;
        copy.resultSizeRange = this.resultSizeRange;
        copy.requiredVariableInputs = this.requiredVariableInputs;
        copy.implementation = this.implementation;
        copy.authDependent = this.authDependent;
        // CreateAndPopulateTempTableExecutionNode fields
        copy.inputVarNames = this.inputVarNames;
        copy.tempTableName = this.tempTableName;
        copy.tempTableColumnMetaData = this.tempTableColumnMetaData;
        copy.connection = newConnection;
        return copy;
    }
}
