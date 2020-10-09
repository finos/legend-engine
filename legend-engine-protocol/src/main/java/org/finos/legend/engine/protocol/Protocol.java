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

package org.finos.legend.engine.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

public class Protocol
{
    public String name;
    public String version;

    public Protocol()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public Protocol(String name, String version)
    {
        this.name = name;
        this.version = version;
    }

    @Override
    @JsonIgnore
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        Protocol protocol = (Protocol) o;
        return Objects.equals(name, protocol.name) && Objects.equals(version, protocol.version);
    }

    @Override
    @JsonIgnore
    public int hashCode()
    {
        return Objects.hash(name, version);
    }
}
