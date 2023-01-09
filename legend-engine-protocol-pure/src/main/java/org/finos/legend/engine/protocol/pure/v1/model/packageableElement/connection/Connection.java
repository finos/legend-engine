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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type")
@JsonSubTypes({
        // Pointer to `PackageableConnection`
        @JsonSubTypes.Type(value = ConnectionPointer.class, name = "connectionPointer"),
        // M2M
        @JsonSubTypes.Type(value = ModelConnection.class, name = "ModelConnection"),
        @JsonSubTypes.Type(value = ModelChainConnection.class, name = "ModelChainConnection"),
        @JsonSubTypes.Type(value = JsonModelConnection.class, name = "JsonModelConnection"),
        @JsonSubTypes.Type(value = XmlModelConnection.class, name = "XmlModelConnection"),
})
public abstract class Connection
{
    public String element;
    public SourceInformation elementSourceInformation;
    public SourceInformation sourceInformation;

    public <T> T accept(ConnectionVisitor<T> connectionVisitor)
    {
        return connectionVisitor.visit(this);
    }
}
