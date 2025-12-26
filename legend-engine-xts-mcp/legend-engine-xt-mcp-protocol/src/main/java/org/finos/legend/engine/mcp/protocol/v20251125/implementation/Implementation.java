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

package org.finos.legend.engine.mcp.protocol.v20251125.implementation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.finos.legend.engine.mcp.protocol.v20251125.icon.Icon;

import java.util.List;
import java.util.Objects;

public class Implementation
{
    @JsonProperty
    private final String description;

    @JsonProperty
    private final List<Icon> icons;

    @JsonProperty
    private final String name;

    @JsonProperty
    private final String title;

    @JsonProperty
    private final String version;

    @JsonProperty
    private final String websiteUrl;

    public Implementation(final String description, final List<Icon> icons, final String name, final String title, final String version, final String websiteUrl)
    {
        this.description = description;
        this.icons = icons;
        this.name = Objects.requireNonNull(name);
        this.title = title;
        this.version = Objects.requireNonNull(version);
        this.websiteUrl = websiteUrl;
    }

    public String getDescription()
    {
        return this.description;
    }

    public List<Icon> getIcons()
    {
        return this.icons;
    }

    public String getName()
    {
        return this.name;
    }

    public String getTitle()
    {
        return this.title;
    }

    public String getVersion()
    {
        return this.version;
    }

    public String getWebsiteUrl()
    {
        return this.websiteUrl;
    }
}
