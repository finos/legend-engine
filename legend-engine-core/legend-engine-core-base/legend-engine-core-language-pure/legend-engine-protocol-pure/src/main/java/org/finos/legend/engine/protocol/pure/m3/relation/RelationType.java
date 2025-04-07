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

package org.finos.legend.engine.protocol.pure.m3.relation;

import org.finos.legend.engine.protocol.pure.m3.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.type.Type;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class RelationType implements Type
{
    public SourceInformation sourceInformation;
    public List<Column> columns = Collections.emptyList();

    public RelationType()
    {
    }

    public RelationType(List<Column> columns)
    {
        this.columns = columns;
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof RelationType))
        {
            return false;
        }
        RelationType that = (RelationType) o;
        return Objects.equals(columns, that.columns);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(columns);
    }
}
