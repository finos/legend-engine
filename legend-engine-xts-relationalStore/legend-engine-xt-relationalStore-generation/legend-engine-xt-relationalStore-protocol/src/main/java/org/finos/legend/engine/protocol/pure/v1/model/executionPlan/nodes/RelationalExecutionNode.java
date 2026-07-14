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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.util.Collections;
import java.util.List;

@Deprecated
public class RelationalExecutionNode extends ExecutionNode
{
    public String sqlComment;
    public String sqlQuery;
    public String onConnectionCloseCommitQuery;
    public String onConnectionCloseRollbackQuery;
    public DatabaseConnection connection;
    public List<SQLResultColumn> resultColumns = Collections.emptyList();

    @Override
    public <T> T accept(ExecutionNodeVisitor<T> executionNodeVisitor)
    {
        return executionNodeVisitor.visit(this);
    }

    /* SQL/Database APIs */
    public String sqlComment()
    {
        return this.sqlComment;
    }

    public String sqlQuery()
    {
        return this.sqlQuery;
    }

    @JsonIgnore
    public String getDatabaseTypeName()
    {
        return this.connection.type.name();
    }

    @JsonIgnore
    public String getDatabaseTimeZone()
    {
        return this.connection.timeZone;
    }

    @JsonIgnore
    public List<SQLResultColumn> getSQLResultColumns()
    {
        return ListIterate.collect(this.resultColumns, SQLResultColumn::new);
    }

    /**
     * Returns a shallow copy of this node with the {@code connection} field replaced by
     * the supplied value. All other fields (including inherited {@link ExecutionNode}
     * fields) are copied by reference. Used by the relational executor to enrich a
     * plan-level connection with identity/allocation-derived state without mutating
     * the shared, cached plan.
     */
    @JsonIgnore
    public RelationalExecutionNode shallowCopyWithConnection(DatabaseConnection newConnection)
    {
        RelationalExecutionNode copy = new RelationalExecutionNode();
        // ExecutionNode fields
        copy.resultType = this.resultType;
        copy.executionNodes = this.executionNodes;
        copy.resultSizeRange = this.resultSizeRange;
        copy.requiredVariableInputs = this.requiredVariableInputs;
        copy.implementation = this.implementation;
        copy.authDependent = this.authDependent;
        // RelationalExecutionNode fields
        copy.sqlComment = this.sqlComment;
        copy.sqlQuery = this.sqlQuery;
        copy.onConnectionCloseCommitQuery = this.onConnectionCloseCommitQuery;
        copy.onConnectionCloseRollbackQuery = this.onConnectionCloseRollbackQuery;
        copy.connection = newConnection;
        copy.resultColumns = this.resultColumns;
        return copy;
    }
}
