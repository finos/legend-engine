// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.store.relational.connection.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StorePattern {

    private final String catalog;

    private final String schemaPattern;

    private final String tablePattern;

    private final boolean escapeSchemaPattern;

    private final boolean escapteTablePattern;



    @JsonCreator
    public StorePattern(
            @JsonProperty("catalog") String catalog,
            @JsonProperty("schemaPattern") String schemaPattern,
            @JsonProperty("tablePattern") String tablePattern,
            @JsonProperty("escapeSchemaPattern") boolean escapeSchemaPattern,
            @JsonProperty("escapteTablePattern") boolean escapeTablePattern
    )
    {
        this.catalog = catalog;
        this.schemaPattern = schemaPattern;
        this.tablePattern = tablePattern;
        this.escapeSchemaPattern = escapeSchemaPattern;
        this.escapteTablePattern = escapeTablePattern;
    }

    public StorePattern(String schemaPattern, String tablePattern, boolean escapeSchemaPattern, boolean escapeTablePattern)
    {
        this(null, schemaPattern, tablePattern, escapeSchemaPattern, escapeTablePattern);
    }


    public StorePattern(String schemaPattern, String tablePattern)
    {
        this(null, schemaPattern, tablePattern, false, false);
    }

    public StorePattern(String catalog)
    {
        this(catalog, null, null, false, false);
    }

    public String getSchemaPattern() {
        return schemaPattern;
    }

    public String getCatalog() {
        return catalog;
    }

    public String getTablePattern() {
        return tablePattern;
    }

    public boolean isEscapeSchemaPattern() {
        return escapeSchemaPattern;
    }

    public boolean isEscapeTablePattern() {
        return escapteTablePattern;
    }

    public StorePattern withNewCatalog(String catalog)
    {
        return new StorePattern(catalog, this.schemaPattern, this.tablePattern, this.escapeSchemaPattern, this.escapteTablePattern);
    }

}
