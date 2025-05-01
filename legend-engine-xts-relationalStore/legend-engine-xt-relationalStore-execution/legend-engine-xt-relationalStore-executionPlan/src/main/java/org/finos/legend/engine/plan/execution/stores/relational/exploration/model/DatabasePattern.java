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
//

package org.finos.legend.engine.plan.execution.stores.relational.exploration.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DatabasePattern
{

    private final String catalog;

    private final String schemaPattern;

    private final String tablePattern;

    private final String functionPattern;

    private final boolean escapeSchemaPattern;

    private final boolean escapeTablePattern;

    private final boolean escapeFunctionPattern;

    @JsonCreator
    public DatabasePattern(
            @JsonProperty("catalog") String catalog,
            @JsonProperty("schemaPattern") String schemaPattern,
            @JsonProperty("tablePattern") String tablePattern,
            @JsonProperty("functionPattern") String functionPattern,
            @JsonProperty("escapeSchemaPattern") boolean escapeSchemaPattern,
            @JsonProperty("escapeTablePattern") @JsonAlias("escapteTablePattern") boolean escapeTablePattern,
            @JsonProperty("escapeFunctionPattern") boolean escapeFunctionPattern
    )
    {
        this.catalog = catalog;
        this.schemaPattern = schemaPattern;
        this.tablePattern = tablePattern;
        this.functionPattern = functionPattern;
        this.escapeSchemaPattern = escapeSchemaPattern;
        this.escapeTablePattern = escapeTablePattern;
        this.escapeFunctionPattern = escapeFunctionPattern;
    }

    public DatabasePattern(String schemaPattern, String tablePattern, boolean escapeSchemaPattern, boolean escapeTablePattern)
    {
        this(null, schemaPattern, tablePattern, null, escapeSchemaPattern, escapeTablePattern, false);
    }


    public DatabasePattern(String schemaPattern, String tablePattern)
    {
        this(null, schemaPattern, tablePattern, null, false, false, false);
    }

    public DatabasePattern(String catalog)
    {
        this(catalog, null, null, null, false, false, false);
    }

    public String getSchemaPattern()
    {
        return schemaPattern;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public String getTablePattern()
    {
        return tablePattern;
    }

    public String getFunctionPattern()
    {
        return functionPattern;
    }

    public boolean isEscapeSchemaPattern()
    {
        return escapeSchemaPattern;
    }

    public boolean isEscapeTablePattern()
    {
        return escapeTablePattern;
    }

    public boolean isEscapeFunctionPattern()
    {
        return escapeFunctionPattern;
    }

    public DatabasePattern withNewCatalog(String catalog)
    {
        return new DatabasePattern(catalog, this.schemaPattern, this.tablePattern, this.functionPattern, this.escapeSchemaPattern, this.escapeTablePattern, this.escapeFunctionPattern);
    }

}
