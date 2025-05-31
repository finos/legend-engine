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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.IcebergProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.TableOrigin;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.tabletypes.IcebergTableType;
import org.finos.legend.engine.persistence.components.relational.sqldom.tabletypes.TableType;

import java.util.Map;

public class DatasetAdditionalPropertiesVisitor extends org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.DatasetAdditionalPropertiesVisitor
{

    protected void handleTableOrigin(PhysicalPlanNode prev, TableOrigin tableOrigin)
    {
        TableType tableType = mapTableType(tableOrigin);
        if (tableType != null)
        {
            prev.push(tableType);
        }
    }

    protected void handleTags(PhysicalPlanNode prev, Map<String, String> tags)
    {
        prev.push(tags);
    }

    protected void handleIcebergProperties(PhysicalPlanNode prev, IcebergProperties icebergProperties)
    {
        prev.push(new org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.common.IcebergProperties(
            icebergProperties.catalog(),
            icebergProperties.externalVolume(),
            icebergProperties.baseLocation(),
            icebergProperties.catalogSync()
        ));
    }

    private TableType mapTableType(TableOrigin tableOrigin)
    {
        switch (tableOrigin)
        {
            case NATIVE: return null;
            case ICEBERG: return new IcebergTableType();
            default: throw new UnsupportedOperationException("Unsupported Table origin : " + tableOrigin);
        }
    }
}
