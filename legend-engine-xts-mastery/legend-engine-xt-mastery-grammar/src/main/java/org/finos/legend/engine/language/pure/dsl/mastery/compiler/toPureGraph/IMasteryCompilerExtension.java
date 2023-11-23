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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.dsl.generation.compiler.toPureGraph.GenerationCompilerExtension;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.CredentialSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authorization.Authorization;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.runtime.MasteryRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_CredentialSecret;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authorization_Authorization;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_runtime_MasteryRuntime;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_trigger_Trigger;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;

public interface IMasteryCompilerExtension extends GenerationCompilerExtension
{
    static List<IMasteryCompilerExtension> getExtensions()
    {
        return Lists.mutable.ofAll(ServiceLoader.load(IMasteryCompilerExtension.class));
    }

    static Root_meta_pure_mastery_metamodel_trigger_Trigger process(Trigger trigger, List<Function2<Trigger, CompileContext, Root_meta_pure_mastery_metamodel_trigger_Trigger>> processors, CompileContext context)
    {
        return process(trigger, processors, context, "trigger", trigger.sourceInformation);
    }

    static Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol process(AcquisitionProtocol acquisitionProtocol, List<Function2<AcquisitionProtocol, CompileContext, Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol>> processors, CompileContext context)
    {
        return process(acquisitionProtocol, processors, context, "acquisition protocol", acquisitionProtocol.sourceInformation);
    }

    static Root_meta_pure_mastery_metamodel_connection_Connection process(Connection connection, List<Function2<Connection, CompileContext, Root_meta_pure_mastery_metamodel_connection_Connection>> processors, CompileContext context)
    {
        return process(connection, processors, context, "connection", connection.sourceInformation);
    }

    static Root_meta_pure_mastery_metamodel_runtime_MasteryRuntime process(MasteryRuntime masteryRuntime, List<Function2<MasteryRuntime, CompileContext, Root_meta_pure_mastery_metamodel_runtime_MasteryRuntime>> processors, CompileContext context)
    {
        return process(masteryRuntime, processors, context, "mastery runtime", masteryRuntime.sourceInformation);
    }

    static Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy process(AuthenticationStrategy authenticationStrategy, List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy>> processors, CompileContext context)
    {
        return process(authenticationStrategy, processors, context, "authentication strategy", authenticationStrategy.sourceInformation);
    }

    static Root_meta_pure_mastery_metamodel_authentication_CredentialSecret process(CredentialSecret secret, List<Function2<CredentialSecret, CompileContext, Root_meta_pure_mastery_metamodel_authentication_CredentialSecret>> processors, CompileContext context)
    {
        return process(secret, processors, context, "secret", secret.sourceInformation);
    }

    static Root_meta_pure_mastery_metamodel_authorization_Authorization process(Authorization authorization, List<Function2<Authorization, CompileContext, Root_meta_pure_mastery_metamodel_authorization_Authorization>> processors, CompileContext context)
    {
        return process(authorization, processors, context, "authorization", authorization.sourceInformation);
    }

    static <T, U> U process(T item, List<Function2<T, CompileContext, U>> processors, CompileContext context, String type, SourceInformation srcInfo)
    {
        return ListIterate
                .collect(processors, processor -> processor.value(item, context))
                .select(Objects::nonNull)
                .getFirstOptional()
                .orElseThrow(() -> new EngineException("Unsupported " + type + " type '" + item.getClass() + "'", srcInfo, EngineErrorType.COMPILATION));
    }

    default List<Function2<Connection, CompileContext, Root_meta_pure_mastery_metamodel_connection_Connection>> getExtraMasteryConnectionProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function2<MasteryRuntime, CompileContext, Root_meta_pure_mastery_metamodel_runtime_MasteryRuntime>> getExtraMasteryRuntimeProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function2<Trigger, CompileContext, Root_meta_pure_mastery_metamodel_trigger_Trigger>> getExtraTriggerProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy>> getExtraAuthenticationStrategyProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function2<CredentialSecret, CompileContext, Root_meta_pure_mastery_metamodel_authentication_CredentialSecret>> getExtraSecretProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function2<AcquisitionProtocol, CompileContext, Root_meta_pure_mastery_metamodel_acquisition_AcquisitionProtocol>> getExtraAcquisitionProtocolProcessors()
    {
        return Collections.emptyList();
    }

    default List<Function2<Authorization, CompileContext, Root_meta_pure_mastery_metamodel_authorization_Authorization>> getExtraAuthorizationProcessors()
    {
        return Collections.emptyList();
    }

    default Set<String> getValidDataProviderTypes()
    {
        return Collections.emptySet();
    }
}
