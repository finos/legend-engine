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

package org.finos.legend.engine.mcp.protocol.v20251125.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.mcp.protocol.v20251125.icon.Icon;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Tool
{
    @JsonProperty
    private final Map<String, Object> _meta;

    @JsonProperty
    private final ToolAnnotations annotations;

    @JsonProperty
    private final String description;

    @JsonProperty
    private final ToolExecution execution;

    @JsonProperty
    private final List<Icon> icons;

    @JsonProperty
    private final Schema inputSchema;

    @JsonProperty
    private final String name;

    @JsonProperty
    private Schema outputSchema;

    @JsonProperty
    private final String title;

    public Tool(final Map<String, Object> _meta, final ToolAnnotations annotations, final String description, final ToolExecution execution, final List<Icon> icons, final Schema inputSchema, final String name, final Schema outputSchema, final String title)
    {
        this._meta = _meta;
        this.annotations = annotations;
        this.description = description;
        this.execution = execution;
        this.icons = icons;
        this.inputSchema = Objects.requireNonNull(inputSchema);
        this.name = Objects.requireNonNull(name);
        this.outputSchema = outputSchema;
        this.title = title;
    }

    public Map<String, Object> get_meta()
    {
        return this._meta;
    }

    public ToolAnnotations getAnnotations()
    {
        return this.annotations;
    }

    public String getDescription()
    {
        return this.description;
    }

    public ToolExecution getExecution()
    {
        return this.execution;
    }

    public List<Icon> getIcons()
    {
        return this.icons;
    }

    public Schema getInputSchema()
    {
        return this.inputSchema;
    }

    public String getName()
    {
        return this.name;
    }

    public Schema getOutputSchema()
    {
        return this.outputSchema;
    }

    public String getTitle()
    {
        return this.title;
    }

    public static class Schema
    {
        @JsonProperty("$schema")
        private final String _schema;

        @JsonProperty
        private final Map<String, Object> properties;

        @JsonProperty
        private final List<String> required;

        @JsonProperty
        private final String type = "object";

        public Schema(final String _schema, final Map<String, Object> properties, final List<String> required)
        {
            this._schema = _schema;
            this.properties = properties;
            this.required = required;
        }

        public String get_schema()
        {
            return this._schema;
        }

        public Map<String, Object> getProperties()
        {
            return this.properties;
        }

        public List<String> getRequired()
        {
            return this.required;
        }

        public String getType()
        {
            return this.type;
        }
    }
}
