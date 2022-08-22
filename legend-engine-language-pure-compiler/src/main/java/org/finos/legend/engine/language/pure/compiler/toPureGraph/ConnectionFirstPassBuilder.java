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

import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.JsonModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelChainConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.ModelStringInput;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.modelToModel.connection.XmlModelConnection;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_JsonModelConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_ModelChainConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_ModelConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_ModelStore_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_XmlModelConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Any_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap;

import java.util.Objects;

public class ConnectionFirstPassBuilder implements ConnectionVisitor<Connection>
{
    private final CompileContext context;

    public ConnectionFirstPassBuilder(CompileContext context)
    {
        this.context = context;
    }

    @Override
    public Connection visit(org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection connection)
    {
        return this.context.getCompilerExtensions().getExtraConnectionValueProcessors().stream()
                .map(processor -> processor.value(connection, this.context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException("Unsupported connection value type '" + connection.getClass() + "'"));
    }

    @Override
    public Connection visit(ConnectionPointer connectionPointer)
    {
        return this.context.resolveConnection(connectionPointer.connection, connectionPointer.sourceInformation);
    }

    @Override
    public Connection visit(ModelConnection modelConnection)
    {
        HelperConnectionBuilder.verifyModelConnectionStore(modelConnection.element, modelConnection.elementSourceInformation);
        MutableMap<Class<?>, List<Root_meta_pure_metamodel_type_Any_Impl>> pureInstancesMap = HelperConnectionBuilder.processModelInput((ModelStringInput) modelConnection.input, this.context, false, null);
        return new Root_meta_pure_mapping_modelToModel_ModelConnection_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::ModelConnection"))
                ._element(Lists.immutable.with(new Root_meta_pure_mapping_modelToModel_ModelStore_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::ModelStore"))))
                ._instances(new PureMap(pureInstancesMap));
    }

    @Override
    public Connection visit(JsonModelConnection jsonModelConnection)
    {
        HelperConnectionBuilder.verifyModelConnectionStore(jsonModelConnection.element, jsonModelConnection.elementSourceInformation);
        return new Root_meta_pure_mapping_modelToModel_JsonModelConnection_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::JsonModelConnection"))
                ._element(Lists.immutable.with(new Root_meta_pure_mapping_modelToModel_ModelStore_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::ModelStore"))))
                ._class(this.context.resolveClass(jsonModelConnection._class, jsonModelConnection.classSourceInformation))
                ._url(jsonModelConnection.url);
    }

    @Override
    public Connection visit(XmlModelConnection xmlModelConnection)
    {
        HelperConnectionBuilder.verifyModelConnectionStore(xmlModelConnection.element, xmlModelConnection.elementSourceInformation);
        return new Root_meta_pure_mapping_modelToModel_XmlModelConnection_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::XmlModelConnection"))
                ._element(Lists.immutable.with(new Root_meta_pure_mapping_modelToModel_ModelStore_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::ModelStore"))))
                ._class(this.context.resolveClass(xmlModelConnection._class, xmlModelConnection.classSourceInformation))
                ._url(xmlModelConnection.url);
    }

    @Override
    public Connection visit(ModelChainConnection modelChainConnection)
    {
        HelperConnectionBuilder.verifyModelConnectionStore(modelChainConnection.element, modelChainConnection.elementSourceInformation);
        return new Root_meta_pure_mapping_modelToModel_ModelChainConnection_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::ModelChainConnection"))
                ._element(Lists.immutable.with(new Root_meta_pure_mapping_modelToModel_ModelStore_Impl("", null, context.pureModel.getClass("meta::pure::mapping::modelToModel::ModelStore"))))
                ._mappings(ListIterate.collect(modelChainConnection.mappings, m -> this.context.resolveMapping(m, modelChainConnection.mappingsSourceInformation)));
    }
}
