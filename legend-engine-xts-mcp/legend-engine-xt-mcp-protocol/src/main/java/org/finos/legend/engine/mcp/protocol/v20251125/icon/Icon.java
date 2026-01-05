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

package org.finos.legend.engine.mcp.protocol.v20251125.icon;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class Icon
{
    @JsonProperty
    private final String mimeType;

    @JsonProperty
    private final List<String> sizes;

    @JsonProperty
    private final String src;

    @JsonProperty
    private final Theme theme;

    public Icon(final String mimeType, final List<String> sizes, final String src, final Theme theme)
    {
        this.mimeType = mimeType;
        this.sizes = sizes;
        this.src = Objects.requireNonNull(src);
        this.theme = theme;
    }

    public String getMimeType()
    {
        return this.mimeType;
    }

    public List<String> getSizes()
    {
        return this.sizes;
    }

    public String getSrc()
    {
        return this.src;
    }

    public Theme getTheme()
    {
        return this.theme;
    }

    public enum Theme
    {
        dark,
        light,
    }
}