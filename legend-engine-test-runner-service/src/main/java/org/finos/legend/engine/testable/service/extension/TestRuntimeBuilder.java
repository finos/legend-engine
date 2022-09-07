//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.testable.service.extension;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.DataElementReference;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.data.DataElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.IdentifiedConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.LegacyRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.Runtime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.RuntimePointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.StoreConnections;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.ConnectionTestData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.service.TestData;
import org.finos.legend.engine.testable.service.connection.TestConnectionBuilder;

import java.io.Closeable;
import java.util.List;

public class TestRuntimeBuilder
{
    public static Pair<EngineRuntime, List<Closeable>> getTestRuntimeAndClosableResources(Runtime runtime, TestData testData, PureModelContextData pureModelContextData)
    {
        List<Closeable> closeables = Lists.mutable.empty();
        EngineRuntime engineRuntime = resolveRuntime(runtime, pureModelContextData);

        EngineRuntime testRuntime = new EngineRuntime();
        testRuntime.mappings = engineRuntime.mappings;
        testRuntime.connections = Lists.mutable.empty();

        for (StoreConnections storeConnections : engineRuntime.connections)
        {
            StoreConnections testStoreConnections = new StoreConnections();
            testStoreConnections.store = storeConnections.store;
            testStoreConnections.storeConnections = Lists.mutable.empty();

            for (IdentifiedConnection identifiedConnection : storeConnections.storeConnections)
            {
                ConnectionTestData connectionTestData = ListIterate.detect(testData.connectionsTestData, connectionData -> connectionData.id.equals(identifiedConnection.id));

                EmbeddedData embeddedData = null;
                if (connectionTestData != null)
                {
                    if (connectionTestData.data instanceof DataElementReference)
                    {
                        DataElement dataElement = Iterate.detect(pureModelContextData.getElementsOfType(DataElement.class), e -> ((DataElementReference) connectionTestData.data).dataElement.equals(e.getPath()));
                        embeddedData = dataElement.data;
                    }
                    else
                    {
                        embeddedData = connectionTestData.data;
                    }
                }

                Pair<Connection, List<Closeable>> connectionWithCloseables = identifiedConnection.connection.accept(new TestConnectionBuilder(embeddedData, pureModelContextData));

                closeables.addAll(connectionWithCloseables.getTwo());

                IdentifiedConnection testIdentifiedConnection = new IdentifiedConnection();
                testIdentifiedConnection.id = identifiedConnection.id;
                testIdentifiedConnection.connection = connectionWithCloseables.getOne();

                testStoreConnections.storeConnections.add(testIdentifiedConnection);
            }

            testRuntime.connections.add(testStoreConnections);
        }

        return Tuples.pair(testRuntime, closeables);
    }

    private static EngineRuntime resolveRuntime(Runtime runtime, PureModelContextData pureModelContextData)
    {
        if (runtime instanceof EngineRuntime)
        {
            return (EngineRuntime) runtime;
        }
        if (runtime instanceof LegacyRuntime)
        {
            return ((LegacyRuntime) runtime).toEngineRuntime();
        }
        if (runtime instanceof RuntimePointer)
        {
            String runtimeFullPath = ((RuntimePointer) runtime).runtime;
            PackageableElement found = Iterate.detect(pureModelContextData.getElements(), e -> runtimeFullPath.equals(e.getPath()));
            if (!(found instanceof PackageableRuntime))
            {
                throw new RuntimeException("Can't find runtime '" + runtimeFullPath + "'");
            }
            return ((PackageableRuntime) found).runtimeValue;
        }
        throw new UnsupportedOperationException("Unsupported runtime type: " + runtime.getClass().getName());
    }
}
