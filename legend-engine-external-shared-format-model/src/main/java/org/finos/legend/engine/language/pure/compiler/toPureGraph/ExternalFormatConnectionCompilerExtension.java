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
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.Processor;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalSource;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.UrlStreamExternalSource;
import org.finos.legend.engine.shared.core.function.Procedure3;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.*;
import org.finos.legend.pure.m3.coreinstance.meta.pure.store.Store;

import java.util.Collections;
import java.util.List;

public class ExternalFormatConnectionCompilerExtension implements IExternalFormatCompilerExtension
{
    @Override
    public CompilerExtension build()
    {
        return new ExternalFormatConnectionCompilerExtension();
    }

    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors()
    {
        return Lists.immutable.empty();
    }

    @Override
    public List<Function2<Connection, CompileContext, Root_meta_pure_runtime_Connection>> getExtraConnectionValueProcessors()
    {
        return Collections.singletonList(
                (connectionValue, context) ->
                {
                    if (connectionValue instanceof ExternalFormatConnection)
                    {
                        return new Root_meta_external_shared_format_executionPlan_ExternalFormatConnection_Impl("", null, context.pureModel.getClass("meta::external::shared::format::executionPlan::ExternalFormatConnection"));
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Procedure3<Connection, Root_meta_pure_runtime_Connection, CompileContext>> getExtraConnectionSecondPassProcessors()
    {
        return Collections.singletonList(
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
        return Collections.singletonList((spec, context) ->
        {
            if (spec instanceof UrlStreamExternalSource)
            {
                UrlStreamExternalSource urlStreamExternalSource = (UrlStreamExternalSource) spec;
                return new Root_meta_external_shared_format_executionPlan_UrlStreamExternalSource_Impl("", null, context.pureModel.getClass("meta::external::shared::format::executionPlan::UrlStreamExternalSource"))
                        ._url(urlStreamExternalSource.url);
            }
            return null;
        });
    }
}
