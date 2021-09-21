// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.protocol.pure.v1.model.context;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class AlloySDLC extends SDLC
{
    @JsonProperty(value = "project")
    public String project;

    @JsonProperty(value = "groupId")
    public String groupId;

    @JsonProperty(value = "artifactId")
    public String artifactId;

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if ((o == null) || (o.getClass() != this.getClass()))
        {
            return false;
        }
        AlloySDLC that = (AlloySDLC) o;
        return Objects.equals(this.project, that.project) && Objects.equals(this.version, that.version) && Objects.equals(this.groupId, that.groupId) && Objects.equals(this.artifactId, that.artifactId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(this.project) + 89 * Objects.hashCode(this.version)+ 17 * Objects.hashCode(this.groupId) + 17 * Objects.hashCode(artifactId);
    }
}
