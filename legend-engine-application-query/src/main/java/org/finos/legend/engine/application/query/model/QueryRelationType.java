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

package org.finos.legend.engine.application.query.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;
import java.util.Map;

/**
 * The columns a saved query returns, kept with it so that a client can know them without compiling
 * the query's project. The client computes them when it saves the query, and leaves them out when it
 * can't; the store keeps them as sent and never reads them.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryRelationType
{
    /**
     * The columns as the engine's lambda relation type gives them, kept as sent: each column's name,
     * generic type (type parameters included, e.g. Varchar(40)) and multiplicity
     */
    public List<Map<String, ?>> columns;

    /**
     * The release of the query's project the columns were computed on: the query's own version, or
     * for a query on "latest", the release it resolved to
     */
    public String versionId;

    @Override
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
