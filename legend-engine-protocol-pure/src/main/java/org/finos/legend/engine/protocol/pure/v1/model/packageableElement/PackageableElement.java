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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.Mapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mapping.MappingClass;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Mapping.class, name = "mapping"),
        @JsonSubTypes.Type(value = MappingClass.class, name = "mappingClass"),
        @JsonSubTypes.Type(value = PackageableConnection.class, name = "connection"),
        @JsonSubTypes.Type(value = PackageableRuntime.class, name = "runtime"),
        @JsonSubTypes.Type(value = DataElement.class, name = "dataElement")
})
public abstract class PackageableElement
{
    @JsonProperty(value = "package")
    public String _package;
    public String name;
    public SourceInformation sourceInformation;

    @JsonIgnore
    public String getPath()
    {
        return this._package == null || this._package.isEmpty() ? this.name : this._package + "::" + this.name;
    }

    public abstract <T> T accept(PackageableElementVisitor<T> visitor);
}
