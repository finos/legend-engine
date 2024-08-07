// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.external.relation.compiled.natives.shared;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.ColSpec;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortDirection;
import org.finos.legend.pure.runtime.java.extension.external.relation.shared.SortInfo;

public class Win
{
    MutableList<? extends String> partition = Lists.mutable.empty();
    MutableList<SortInfo> sorts = Lists.mutable.empty();

    public Win(MutableList<? extends String> colSpec, RichIterable<Pair<Enum, String>> sorts)
    {
        this.partition = colSpec;
        this.sorts = sorts.toList().collect(c -> new SortInfo(c.getTwo(), SortDirection.valueOf(c.getOne()._name())));
    }

    public MutableList<? extends String> getPartition()
    {
        return partition;
    }

    public MutableList<SortInfo> getSorts()
    {
        return sorts;
    }
}