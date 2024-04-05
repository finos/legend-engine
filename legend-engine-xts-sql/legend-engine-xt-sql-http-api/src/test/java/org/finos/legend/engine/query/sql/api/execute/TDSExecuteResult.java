// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api.execute;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class TDSExecuteResult
{
    TDSResult result;

    public TDSExecuteResult(@JsonProperty("result") TDSResult result)
    {
        this.result = result;
    }

    @Override
    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public static TDSExecuteResultBuilder builder(List<String> columns)
    {
        return new TDSExecuteResultBuilder(columns);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TDSResultValue
    {
        List<Object> values;

        public TDSResultValue(@JsonProperty("values") List<Object> values)
        {
            this.values = values;
        }

        @Override
        public boolean equals(Object o)
        {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode()
        {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class TDSResult
    {
        List<String> columns;
        List<TDSResultValue> rows;

        public TDSResult(@JsonProperty("columns") List<String> columns, @JsonProperty("rows") List<TDSResultValue> rows)
        {
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        public boolean equals(Object o)
        {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode()
        {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this);
        }
    }

    static class TDSExecuteResultBuilder
    {
        private List<String> columns;
        private List<TDSResultValue> values = FastList.newList();

        public TDSExecuteResultBuilder(List<String> columns)
        {
            this.columns = columns;
        }

        public TDSExecuteResult build()
        {
            return new TDSExecuteResult(new TDSResult(columns, values));
        }

        public TDSExecuteResultBuilder addRow(List<Object> values)
        {
            this.values.add(new TDSResultValue(values));
            return this;
        }
    }
}
