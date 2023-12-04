// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.IcebergProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.TableOrigin;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.CachedTableType;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.MemoryTableType;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.TableType;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.TemporaryTableType;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.TransientTableType;

import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.Map;

public class DatasetAdditionalPropertiesVisitor implements LogicalPlanVisitor<org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, DatasetAdditionalProperties current, VisitorContext context)
    {
        // Handle table type
        if (current.tableType().isPresent())
        {
            handleTableType(prev, current.tableType().get());
        }

        // Handle table origin
        if (current.tableOrigin().isPresent())
        {
            handleTableOrigin(prev, current.tableOrigin().get());
        }

        // Handle tags
        if (!current.tags().isEmpty())
        {
            handleTags(prev, current.tags());
        }

        // Handle iceberg properties
        if (current.icebergProperties().isPresent())
        {
            handleIcebergProperties(prev, current.icebergProperties().get());
        }

        return new VisitorResult(null);
    }

    protected void handleTableType(PhysicalPlanNode prev, org.finos.legend.engine.persistence.components.logicalplan.datasets.TableType tableType)
    {
        TableType mappedTableType = mapTableType(tableType);
        prev.push(mappedTableType);
    }

    protected void handleTableOrigin(PhysicalPlanNode prev, TableOrigin tableOrigin)
    {
        switch (tableOrigin)
        {
            // native tables don't need any special marker
            // Other table origins not supported in ANSI, will be implemented in respective Sinks
            case NATIVE: break;
            default: throw new UnsupportedOperationException("Unsupported Table origin : " + tableOrigin);
        }
    }

    protected void handleTags(PhysicalPlanNode prev, Map<String, String> tags)
    {
        throw new UnsupportedOperationException("Tags not supported");
    }

    protected void handleIcebergProperties(PhysicalPlanNode prev, IcebergProperties icebergProperties)
    {
        throw new UnsupportedOperationException("Iceberg properties not supported");
    }

    private TableType mapTableType(org.finos.legend.engine.persistence.components.logicalplan.datasets.TableType tableType)
    {
        switch (tableType)
        {
            case CACHED: return new CachedTableType();
            case MEMORY: return new MemoryTableType();
            case TEMPORARY: return new TemporaryTableType();
            case TRANSIENT: return new TransientTableType();
            default: throw new UnsupportedOperationException("Unknown Table type");
        }
    }
}
