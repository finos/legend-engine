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
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementPointer;
import org.finos.legend.engine.protocol.pure.v1.model.context.PackageableElementType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LegacyRuntime extends Runtime
{
    public List<PackageableElementPointer> mappings = Collections.emptyList();
    public List<Connection> connections = Collections.emptyList();

    @JsonIgnore
    public EngineRuntime toEngineRuntime()
    {
        EngineRuntime engineRuntime = new EngineRuntime();
        AtomicInteger counter = new AtomicInteger(1);
        engineRuntime.mappings = this.mappings;
        ListIterate.forEach(this.connections, connection ->
        {
            IdentifiedConnection identifiedConnection = new IdentifiedConnection();
            identifiedConnection.id = "connection_" + counter; // adhoc connection id creation
            identifiedConnection.connection = connection;
            counter.getAndIncrement();
            // find the current connections by store and update
            if (engineRuntime.getStoreConnections(connection.element) != null)
            {
                engineRuntime.getStoreConnections(connection.element).storeConnections.add(identifiedConnection);
            }
            else
            {
                StoreConnections storeConnections = new StoreConnections();
                storeConnections.storeConnections.add(identifiedConnection);
                PackageableElementPointer storePointer = new PackageableElementPointer();
                storePointer.type = PackageableElementType.STORE;
                storePointer.path = connection.element;
                storeConnections.store = storePointer;
                engineRuntime.connections.add(storeConnections);
            }
        });
        return engineRuntime;
    }
}
