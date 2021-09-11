// Copyright 2021 Goldman Sachs
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

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.ExternalSource;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.external.shared.UrlStreamExternalSource;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_binding_Binding;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_executionPlan_ExternalFormatConnection;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_executionPlan_ExternalFormatConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_executionPlan_ExternalSource;
import org.finos.legend.pure.generated.Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mapping_modelToModel_ModelStore_Impl;
import org.finos.legend.pure.generated.Root_meta_relational_metamodel_Database_Impl;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.List;

public class ExternalFormatConnectionCompilerExtension implements IExternalFormatCompilerExtension
{

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.empty();
    }

    @Override
    public List<Function2<Connection, CompileContext, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection>> getExtraConnectionValueProcessors()
    {
        return Lists.mutable.with(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof ExternalFormatConnection)
                    {
                        return new Root_meta_external_shared_format_executionPlan_ExternalFormatConnection_Impl("");
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Procedure3<Connection, org.finos.legend.pure.m3.coreinstance.meta.pure.runtime.Connection, CompileContext>> getExtraConnectionSecondPassProcessors()
    {
        return Lists.mutable.with(
                (connectionValue, pureConnection, context) ->
                {
                    if (connectionValue instanceof ExternalFormatConnection)
                    {
                        ExternalFormatConnection externalFormatConnection = (ExternalFormatConnection) connectionValue;
                        List<IExternalFormatCompilerExtension> extensions = IExternalFormatCompilerExtension.getExtensions(context);

                        Root_meta_external_shared_format_executionPlan_ExternalFormatConnection connection = (Root_meta_external_shared_format_executionPlan_ExternalFormatConnection) pureConnection;

                        Store store = context.pureModel.getStore(connectionValue.element, connectionValue.elementSourceInformation);
                        if (!(store instanceof Root_meta_external_shared_format_binding_Binding))
                        {
                            throw new EngineException("Store for ExternalFormatConnection must be a Binding", connectionValue.sourceInformation, EngineErrorType.COMPILATION);
                        }
                        connection._element(store);

                        Root_meta_external_shared_format_executionPlan_ExternalSource externalSource = IExternalFormatCompilerExtension.process(
                                externalFormatConnection.externalSource,
                                ListIterate.flatCollect(extensions, IExternalFormatCompilerExtension::getExtraExternalSourceSpecificationProcessors),
                                context);
                        connection._externalSource(externalSource);
                    }
                }
        );
    }

    @Override
    public List<Function2<ExternalSource, CompileContext, Root_meta_external_shared_format_executionPlan_ExternalSource>> getExtraExternalSourceSpecificationProcessors()
    {
        return Lists.mutable.with((spec, context) -> {
            if (spec instanceof UrlStreamExternalSource)
            {
                UrlStreamExternalSource urlStreamExternalSource = (UrlStreamExternalSource) spec;
                return new Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource_Impl("")
                        ._url(urlStreamExternalSource.url);
            }
            return null;
        });
    }
}
