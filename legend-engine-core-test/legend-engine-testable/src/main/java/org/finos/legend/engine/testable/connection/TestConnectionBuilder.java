// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.testable.connection;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.protocol.pure.v1.extension.ConnectionFactoryExtension;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.engine.shared.core.url.DataProtocolHandler;

import javax.ws.rs.core.MediaType;
import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

public class TestConnectionBuilder implements ConnectionVisitor<Pair<Connection, List<Closeable>>>
{
    private EmbeddedData embeddedData;
    private PureModelContextData pureModelContextData;

    public TestConnectionBuilder(EmbeddedData embeddedData, PureModelContextData pureModelContextData)
    {
        this.embeddedData = embeddedData;
        this.pureModelContextData = pureModelContextData;
    }

    @Override
    public Pair<Connection, List<Closeable>> visit(Connection connection)
    {
        MutableList<ConnectionFactoryExtension> factories = Lists.mutable.withAll(ServiceLoader.load(ConnectionFactoryExtension.class));
        return factories
                .collect(f -> f.tryBuildTestConnection(connection, embeddedData))
                .select(Objects::nonNull)
                .select(Optional::isPresent)
                .collect(Optional::get)
                .getFirstOptional()
                .orElseThrow(() ->
                {
                    String errorMessage = embeddedData == null
                            ? "No test data provided for connection type '" + connection.getClass().getSimpleName() + "'. Either you need to provide test data for the connection type or connection type is not supported."
                            : "Unsupported test data type '" + embeddedData.getClass().getSimpleName() + "' with connection type '" + connection.getClass().getSimpleName() + '"';
                    return new UnsupportedOperationException(errorMessage);
                });
    }

    @Override
    public Pair<Connection, List<Closeable>> visit(ConnectionPointer connectionPointer)
    {
        String connectionFullPath = connectionPointer.connection;
        PackageableElement found = Iterate.detect(pureModelContextData.getElements(), e -> connectionFullPath.equals(e.getPath()));
        if (!(found instanceof PackageableConnection))
        {
            throw new RuntimeException("Can't find connection '" + connectionFullPath + "'");
        }
        Connection connection = ((PackageableConnection) found).connectionValue;
        return connection.accept(this);
    }

    @Override
    public Pair<Connection, List<Closeable>> visit(ModelConnection modelConnection)
    {
        throw new UnsupportedOperationException("Test connection builder strategy not found for ModelConnection");
    }

    @Override
    public Pair<Connection, List<Closeable>> visit(JsonModelConnection jsonModelConnection)
    {
        return this.visit((Connection) jsonModelConnection);
    }

    @Override
    public Pair<Connection, List<Closeable>> visit(XmlModelConnection xmlModelConnection)
    {
        return this.visit((Connection) xmlModelConnection);
    }

    @Override
    public Pair<Connection, List<Closeable>> visit(ModelChainConnection modelChainConnection)
    {
        return Tuples.pair(modelChainConnection, Collections.emptyList());
    }
}
