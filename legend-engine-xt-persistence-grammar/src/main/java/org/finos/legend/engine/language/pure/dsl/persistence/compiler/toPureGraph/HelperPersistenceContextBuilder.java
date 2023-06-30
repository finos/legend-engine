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

package org.finos.legend.engine.language.pure.dsl.persistence.compiler.toPureGraph;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionFirstPassBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.ConnectionSecondPassBuilder;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.to.PrimitiveValueSpecificationToObjectVisitor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.parameter.ConnectionValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.parameter.PrimitiveTypeValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.parameter.ServiceParameter;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;

public class HelperPersistenceContextBuilder
{
    private HelperPersistenceContextBuilder()
    {
    }

    public static Root_meta_pure_persistence_metamodel_Persistence buildPersistence(PersistenceContext persistenceContext, CompileContext context)
    {
        String persistence = persistenceContext.persistence;
        String persistencePath = persistence.substring(0, persistence.lastIndexOf("::"));
        String persistenceName = persistence.substring(persistence.lastIndexOf("::") + 2);

        PackageableElement packageableElement = context.pureModel.getOrCreatePackage(persistencePath)._children().detect(c -> persistenceName.equals(c._name()));
        if (packageableElement instanceof Root_meta_pure_persistence_metamodel_Persistence)
        {
            return (Root_meta_pure_persistence_metamodel_Persistence) packageableElement;
        }
        throw new EngineException(String.format("Persistence '%s' is not defined", persistence), persistenceContext.sourceInformation, EngineErrorType.COMPILATION);
    }

    public static Root_meta_pure_persistence_metamodel_context_PersistencePlatform buildPersistencePlatform(PersistencePlatform persistencePlatform, CompileContext context)
    {
        return IPersistenceCompilerExtension.process(persistencePlatform, ListIterate.flatCollect(IPersistenceCompilerExtension.getExtensions(), IPersistenceCompilerExtension::getExtraPersistencePlatformProcessors), context);
    }

    public static Root_meta_pure_persistence_metamodel_service_ServiceParameter buildServiceParameter(ServiceParameter serviceParameter, CompileContext context)
    {
        Root_meta_pure_persistence_metamodel_service_ServiceParameter pureServiceParameter = new Root_meta_pure_persistence_metamodel_service_ServiceParameter_Impl("", null, context.pureModel.getClass("meta::pure::persistence::metamodel::service::ServiceParameter"));
        pureServiceParameter._name(serviceParameter.name);

        if (serviceParameter.value instanceof PrimitiveTypeValue)
        {
            Object value = ((PrimitiveTypeValue) serviceParameter.value).primitiveType.accept(new PrimitiveValueSpecificationToObjectVisitor());
            pureServiceParameter._value(Lists.fixedSize.of(value));
            return pureServiceParameter;
        }
        else if (serviceParameter.value instanceof ConnectionValue)
        {
            Root_meta_pure_runtime_Connection value = buildConnection(((ConnectionValue) serviceParameter.value).connection, context);
            pureServiceParameter._value(Lists.fixedSize.of(value));
            return pureServiceParameter;
        }

        throw new EngineException(String.format("Unable to build service parameter of type '%s'.", serviceParameter.value.getClass()));
    }

    public static Root_meta_pure_runtime_Connection buildConnection(Connection connection, CompileContext context)
    {
        if (connection == null)
        {
            return null;
        }

        Root_meta_pure_runtime_Connection pureConnection = connection.accept(new ConnectionFirstPassBuilder(context));
        connection.accept(new ConnectionSecondPassBuilder(context, pureConnection));
        return pureConnection;
    }
}
