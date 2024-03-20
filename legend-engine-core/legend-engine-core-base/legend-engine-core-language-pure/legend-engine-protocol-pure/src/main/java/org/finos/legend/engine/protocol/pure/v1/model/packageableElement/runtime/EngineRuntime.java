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

package org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;

import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "_type", defaultImpl = EngineRuntime.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EngineRuntime.class, name = "engineRuntime"),
        @JsonSubTypes.Type(value = SingleConnectionEngineRuntime.class, name = "localEngineRuntime")
})
public class EngineRuntime extends Runtime
{
    public List<PackageableElementPointer> mappings = new ArrayList<>();
    public List<StoreConnections> connections = new ArrayList<>();
    public List<ConnectionStores> connectionStores = new ArrayList<>();

    @JsonIgnore
    public StoreConnections getStoreConnections(String store)
    {
        return this.connections.stream().filter(storeConnections -> storeConnections.store.path.equals(store)).findFirst().orElse(null);
    }

    @JsonIgnore
    public ConnectionStores getConnectionStores(String connection)
    {
        return this.connectionStores.stream().filter(connectionStores -> connectionStores.connectionPointer.connection.equals(connection)).findFirst().orElse(null);
    }
}
