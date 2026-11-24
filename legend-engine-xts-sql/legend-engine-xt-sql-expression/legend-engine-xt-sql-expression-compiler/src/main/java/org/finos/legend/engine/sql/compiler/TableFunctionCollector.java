// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.sql.compiler;

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.protocol.sql.metamodel.TableFunction;
import org.finos.legend.engine.protocol.sql.visitors.BaseNodeCollectorVisitor;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

abstract class TableFunctionCollector<T> extends BaseNodeCollectorVisitor<Set<T>>
{
    TableFunctionCollector()
    {
        super(values -> values.stream().flatMap(Collection::stream).collect(Collectors.toSet()), Sets.mutable.empty());
    }

    protected abstract Set<? extends T> collectFrom(TableFunction tableFunction);

    @Override
    public Set<T> visit(TableFunction tableFunction)
    {
        Set<T> collected = Sets.mutable.withAll(collectFrom(tableFunction));
        collected.addAll(super.visit(tableFunction));
        return collected;
    }
}
