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

package org.finos.legend.engine.language.pure.compiler.toPureGraph;

import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;

public class ConnectionSecondPassBuilder implements ConnectionVisitor<Connection>
{
    private final CompileContext context;
    private final Connection pureConnection;

    public ConnectionSecondPassBuilder(CompileContext context, Connection pureConnection)
    {
        this.context = context;
        this.pureConnection = pureConnection;
    }

    @Override
    public Connection visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection connection)
    {
        this.context.getCompilerExtensions().getExtraConnectionSecondPassProcessors().stream()
                .forEach(processor -> processor.value(connection, pureConnection, this.context));
        return pureConnection;
    }

    @Override
    public Connection visit(ConnectionPointer connectionPointer)
    {
        return this.context.resolveConnection(connectionPointer.connection, connectionPointer.sourceInformation);
    }

    @Override
    public Connection visit(ModelConnection modelConnection)
    {
        return pureConnection;
    }

    @Override
    public Connection visit(JsonModelConnection jsonModelConnection)
    {
        return pureConnection;
    }

    @Override
    public Connection visit(XmlModelConnection xmlModelConnection)
    {
        return pureConnection;
    }

    @Override
    public Connection visit(ModelChainConnection modelChainConnection)
    {
        return pureConnection;
    }
}
