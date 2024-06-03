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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.Objects;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;

public class PackageableElementPointer
{
    public PackageableElementType type;
    public String path;
    public SourceInformation sourceInformation;

    public PackageableElementPointer()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public PackageableElementPointer(PackageableElementType type, String path)
    {
        this.type = type;
        this.path = path;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public PackageableElementPointer(String path)
    {
        this.path = path;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PackageableElementPointer(@JsonProperty("type") PackageableElementType type, @JsonProperty("path") String path, @JsonProperty("sourceInformation") SourceInformation sourceInformation)
    {
        this.type = type;
        this.path = path;
        this.sourceInformation = sourceInformation;
    }

    @Override
    public String toString()
    {
        return "{" + type + ", " + path + "}";
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
        PackageableElementPointer that = (PackageableElementPointer) o;
        return type == that.type &&
                Objects.equals(path, that.path);
    }

    @Override
    @JsonIgnore
    public int hashCode()
    {
        return Objects.hash(type, path);
    }

    public static class ToPathSerializerConverter extends StdConverter<PackageableElementPointer, String>
    {
        @Override
        public String convert(PackageableElementPointer value)
        {
            return value.path;
        }
    }
}



