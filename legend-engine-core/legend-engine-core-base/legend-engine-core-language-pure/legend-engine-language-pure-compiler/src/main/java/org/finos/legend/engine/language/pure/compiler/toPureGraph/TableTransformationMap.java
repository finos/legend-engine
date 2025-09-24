// Copyright 2025 Goldman Sachs
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

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.mapping.TablePtr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to encapsulate compilation-related mappings for tables and schemas.
 */
public class TableTransformationMap
{
    private final Map<TablePtr, TablePtr> tableMappings;

    public TableTransformationMap()
    {
        this.tableMappings = new HashMap<>();
    }

    public void addTableMapping(TablePtr oldPtr, TablePtr newPtr)
    {
        this.tableMappings.put(oldPtr, newPtr);
    }

    public Map<TablePtr, TablePtr> getTableMappings()
    {
        return Collections.unmodifiableMap(tableMappings);
    }
}