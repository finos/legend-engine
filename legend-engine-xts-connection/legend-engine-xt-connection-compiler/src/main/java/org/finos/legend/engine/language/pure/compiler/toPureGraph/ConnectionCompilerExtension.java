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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.Connection;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_connection_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_connection_Connection_Impl;

import java.util.Collections;

public class ConnectionCompilerExtension implements CompilerExtension
{
    static final MutableMap<String, Root_meta_pure_metamodel_connection_Connection> connectionsIndex = Maps.mutable.empty();

    @Override
    public CompilerExtension build()
    {
        return new ConnectionCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Collections.singletonList(Processor.newProcessor(
                Connection.class,
                (element, context) ->
                {
                    // @HACKY: new-connection-framework
                    Root_meta_pure_metamodel_connection_Connection metamodel = new Root_meta_pure_metamodel_connection_Connection_Impl(element.name, null, context.pureModel.getClass("meta::pure::metamodel::connection::Connection"))._name(element.name);
                    connectionsIndex.put(context.pureModel.buildPackageString(element._package, element.name), metamodel);
                    metamodel._rawValue(element);
                    return metamodel;
                }));
    }
}
